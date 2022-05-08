package org.gy.framework.launcher.sample;

import org.gy.framework.launcher.sample.point.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@EnableAutoConfiguration
@EnableScheduling
public class Main implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private int step = 1;

    public static void main(String[] args) throws Exception {

        try {
            throw new Exception("Exception before test");
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage(),e);
        }

        SpringApplication.run(Main.class, args);

        try {
            throw new Exception("Exception after spring start test");
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage(),e);
        }

        System.out.println("Hello! Detonator.");
        if (null != args && args.length > 0) {
            logger.info("Args length: {}", args.length);
            for (int i = 0; i < args.length; i++) {
                System.out.println("args " + i + ": " + args[i]);
                Pointer.point("args " + i + ": " + args[i]);
                logger.info("args " + i + ": " + args[i]);
            }
        }
    }

    @RequestMapping("/hello")
    @ResponseBody
    private String hello() {
        return "Hello detonator!";
    }


    @RequestMapping("/logErr")
    private void logErr() {
        Exception e = new Exception("Log error test");
        logger.error(e.getMessage(),e);
    }

    @RequestMapping("/sysErr")
    private void sysErr() {
        System.err.println("err!");
    }

    @RequestMapping("/sysOut")
    @ResponseBody
    private void sysOut() {
        System.err.println("out!");
    }

    @RequestMapping("/out")
    @ResponseBody
    private String out() {
        System.out.println(1111);
        return "";
    }

    @RequestMapping("/sandbox/demo/module/http/metrics/healthyUrl")
    @ResponseBody
    private String healthCheck() {
        return "Hello healthCheck!";
    }

    @RequestMapping("/sandbox/demo/module/http/metrics/readyUrl")
    @ResponseBody
    private String readyCheck() {
        return "Hello readyCheck!";
    }

    @RequestMapping("/sandbox/demo/module/http/metrics/shutdownUrl")
    @ResponseBody
    private String shutdownCheck() {
        return "Hello shutdownCheck!";
    }

    @RequestMapping("isHealthy")
    private void isHealthy() throws Exception{
        if("false".equals(System.getProperty("isHealthy"))){
            throw new Exception("Set healthy check error");
        }
    }

    @RequestMapping("isReady")
    private void isReady() throws Exception{
        if("false".equals(System.getProperty("isReady"))){
            throw new Exception("Set ready check error");
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Sleep 10s");
        TimeUnit.SECONDS.sleep(10);
        logger.info("Sleep 10s end");
    }

    @Scheduled(fixedRate = 1000)
    public void printContinuously (){
        logger.info(String.valueOf(step++));
    }
}
