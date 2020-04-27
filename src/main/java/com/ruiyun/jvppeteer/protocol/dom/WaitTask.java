package com.ruiyun.jvppeteer.protocol.dom;

import com.ruiyun.jvppeteer.protocol.PageEvaluateType;
import com.ruiyun.jvppeteer.protocol.js.JSHandle;
import com.ruiyun.jvppeteer.util.Helper;
import com.ruiyun.jvppeteer.util.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitTask.class);

    private AtomicInteger runCount;

    private boolean terminated;

    private JSHandle promise;

    private DOMWorld domWorld;

    private String polling;

    private int timeout;

    private String predicateBody;

    private Object[] args;

    private Object runningTask;

    public WaitTask(DOMWorld domWorld, String predicateBody, PageEvaluateType type, String title, String polling, int timeout, Object... args) {
        if (Helper.isNumber(polling)) {
            ValidateUtil.assertBoolean(new BigDecimal(polling).compareTo(new BigDecimal(0)) > 0, "Cannot poll with non-positive interval: " + polling);
        } else {
            ValidateUtil.assertBoolean("raf".equals(polling) || "mutation".equals(polling), "Unknown polling option: " + polling);
        }
        this.domWorld = domWorld;
        this.polling = polling;
        this.timeout = timeout;
        this.predicateBody = PageEvaluateType.STRING.equals(type) ? "return (" + predicateBody + ")" : "return (" + predicateBody + ")(...args)";
        this.args = args;
        this.runCount = new AtomicInteger(0);
        domWorld.getWaitTasks().add(this);
        // Since page navigation requires us to re-install the pageScript, we should track
        // timeout on our end.
        //TODO
//        if()
        this.rerun();
    }

    public void rerun() {
        int runcount = runCount.incrementAndGet();
        Exception error = null;
        JSHandle success = null;
        try {
            success = this.domWorld.evaluateHandle(waitForPredicatePageFunction(), PageEvaluateType.FUNCTION, this.predicateBody, this.polling, this.timeout, this.args);
        } catch (Exception e) {
            error = e;
        }
        if (this.terminated || runcount != this.runCount.get()) {
            if (success != null)
                success.dispose();
            return;
        }

        // Ignore timeouts in pageScript - we track timeouts ourselves.
        // If the frame's execution context has already changed, `frame.evaluate` will
        // throw an error - ignore this predicate run altogether.
        if (error == null && this.domWorld.evaluate("s => !s", PageEvaluateType.FUNCTION, success) != null) {
            success.dispose();
            return;
        }

        // When the page is navigated, the promise is rejected.
        // Try again right away.
        if (error != null && error.getMessage().contains("Execution context was destroyed")) {
            this.rerun();
            return;
        }
        if (error != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("", error);
            }
        } else {
            this.promise = success;
        }
        this.cleanup();
    }

    private String waitForPredicatePageFunction() {
//        const predicate = new Function('...args', predicateBody);
//        let timedOut = false;
//        if (timeout)
//            setTimeout(() => timedOut = true, timeout);
//        if (polling === 'raf')
//            return await pollRaf();
//        if (polling === 'mutation')
//            return await pollMutation();
//        if (typeof polling === 'number')
//        return await pollInterval(polling);
//
//        /**
//         * @return {!Promise<*>}
//         */
//        function pollMutation() {
//    const success = predicate.apply(null, args);
//            if (success)
//                return Promise.resolve(success);
//
//            let fulfill;
//    const result = new Promise(x => fulfill = x);
//    const observer = new MutationObserver(mutations => {
//            if (timedOut) {
//                observer.disconnect();
//                fulfill();
//            }
//      const success = predicate.apply(null, args);
//            if (success) {
//                observer.disconnect();
//                fulfill(success);
//            }
//    });
//            observer.observe(document, {
//                    childList: true,
//                    subtree: true,
//                    attributes: true
//    });
//            return result;
        return null;
    }

    public void terminate(RuntimeException e) {
        this.terminated = true;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error("", e);
        }
        this.cleanup();
    }

    private void cleanup() {
        this.domWorld.getWaitTasks().remove(this);
        this.runningTask = null;
    }

    public JSHandle getPromise() {
        return promise;
    }

    public void setPromise(ElementHandle promise) {
        this.promise = promise;
    }
}
