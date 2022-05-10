package org.gy.framework.launcher.sample;

import java.util.concurrent.TimeUnit;
import org.gy.framework.launcher.sample.point.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
public class Main2 {

    private static final Logger logger = LoggerFactory.getLogger(Main2.class);

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main2.class, args);
        logger.info("I'm Main2");
        System.out.println("Hello! launcher.");
        if (null != args && args.length > 0) {
            logger.info("Args length: {}", args.length);
            for (int i = 0; i < args.length; i++) {
                System.out.println("args " + i + ": " + args[i]);
                Pointer.point("args " + i + ": " + args[i]);
                logger.info("args " + i + ": " + args[i]);
            }
        }
        TimeUnit.DAYS.sleep(365);
    }



    @RequestMapping("/logError")
    @ResponseBody
    private String logError() {
        Exception e = new Exception("test");
        logger.error(e.getMessage(),e);
        return "";
    }

    @RequestMapping("/out")
    @ResponseBody
    private String out() {
        System.out.println(1111);
        return "";
    }
}
