package com.rx.geminipro.services

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

class ChatMonitorInjector(
    private val commandExecutor: CommandExecutor
) {

    companion object {
        private const val TAG = "ChatMonitorInjector"
        private const val INTERFACE_NAME = "GeminiHands"

        // JavaScript to inject into Gemini WebView to monitor responses
        val MONITOR_SCRIPT = """
            (function() {
                if (window._geminiHandsInitialized) return;
                window._geminiHandsInitialized = true;
                
                // Monitor DOM changes to detect new Gemini responses
                const observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) {
                                // Look for response containers (Gemini AI Studio format)
                                const responseElements = node.querySelectorAll ? 
                                    node.querySelectorAll('[class*="response"], [class*="message"], [class*="model-response"], [data-message-author="model"]') : [];
                                
                                responseElements.forEach(function(el) {
                                    const text = el.innerText || el.textContent;
                                    if (text && text.trim().length > 0) {
                                        window.GeminiHands.onGeminiResponse(text.trim());
                                    }
                                });
                                
                                // Also check the node itself
                                if (node.getAttribute && (
                                    node.getAttribute('data-message-author') === 'model' ||
                                    (node.className && node.className.includes && node.className.includes('model'))
                                )) {
                                    const text = node.innerText || node.textContent;
                                    if (text && text.trim().length > 0) {
                                        window.GeminiHands.onGeminiResponse(text.trim());
                                    }
                                }
                            }
                        });
                    });
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
                
                // Also poll for new messages periodically as backup
                let lastResponseText = '';
                setInterval(function() {
                    const responses = document.querySelectorAll('[data-message-author="model"], [class*="model-response"]');
                    if (responses.length > 0) {
                        const lastResponse = responses[responses.length - 1];
                        const text = lastResponse.innerText || lastResponse.textContent;
                        if (text && text !== lastResponseText && text.trim().length > 0) {
                            lastResponseText = text;
                            window.GeminiHands.onGeminiResponse(text.trim());
                        }
                    }
                }, 2000);
                
                console.log('Gemini Hands monitor initialized');
            })();
        """.trimIndent()

        // JavaScript to auto-send a message to Gemini
        fun getAutoSendScript(message: String): String {
            val escapedMessage = message.replace("'", "\\'").replace("\n", "\\n")
            return """
                (function() {
                    // Find the input field
                    const inputSelectors = [
                        'textarea[aria-label*="prompt"]',
                        'textarea[placeholder*="Enter"]',
                        'div[contenteditable="true"]',
                        'textarea',
                        'input[type="text"]'
                    ];
                    
                    let input = null;
                    for (const selector of inputSelectors) {
                        input = document.querySelector(selector);
                        if (input) break;
                    }
                    
                    if (input) {
                        // Set the text
                        if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') {
                            input.value = '$escapedMessage';
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                        } else {
                            input.innerText = '$escapedMessage';
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                        
                        // Find and click send button
                        setTimeout(function() {
                            const sendSelectors = [
                                'button[aria-label*="Send"]',
                                'button[aria-label*="send"]',
                                'button[class*="send"]',
                                'button[type="submit"]'
                            ];
                            
                            let sendBtn = null;
                            for (const selector of sendSelectors) {
                                sendBtn = document.querySelector(selector);
                                if (sendBtn) break;
                            }
                            
                            if (sendBtn) {
                                sendBtn.click();
                                window.GeminiHands.onMessageSent('$escapedMessage');
                            }
                        }, 500);
                    }
                })();
            """.trimIndent()
        }
    }

    // JavaScript interface that receives callbacks from the WebView
    inner class GeminiHandsInterface {
        @JavascriptInterface
        fun onGeminiResponse(response: String) {
            Log.d(TAG, "Gemini response received: ${response.take(100)}...")
            
            // Process the response for commands
            commandExecutor.processGeminiResponse(response)
            
            // Log it
            ActionLogger.getInstance()?.logAction(
                "gemini_response",
                response.take(500),
                "processed"
            )
        }

        @JavascriptInterface
        fun onMessageSent(message: String) {
            Log.d(TAG, "Auto-message sent to Gemini: $message")
            ActionLogger.getInstance()?.logAction(
                "auto_message_sent",
                message,
                "sent"
            )
        }

        @JavascriptInterface
        fun onError(error: String) {
            Log.e(TAG, "JS Error: $error")
        }
    }

    fun getJavaScriptInterface(): GeminiHandsInterface {
        return GeminiHandsInterface()
    }

    fun injectMonitor(webView: WebView) {
        webView.evaluateJavascript(MONITOR_SCRIPT, null)
        Log.d(TAG, "Monitor script injected")
    }

    fun sendMessageToGemini(webView: WebView, message: String) {
        webView.evaluateJavascript(getAutoSendScript(message), null)
        Log.d(TAG, "Auto-sending message: $message")
    }
}
