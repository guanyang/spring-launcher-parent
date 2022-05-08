package org.gy.framework.launcher.sample.point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pointer {

    private static final Logger logger = LoggerFactory.getLogger(Pointer.class);


    public static void point(String str){
        logger.info(str);
    }
}
