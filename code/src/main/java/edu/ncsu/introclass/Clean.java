package edu.ncsu.introclass;

import edu.ncsu.preprocess.DeadCode;

import java.util.logging.Logger;

public class Clean {

    private static Logger LOGGER = Logger.getLogger(Clean.class.getName());

    public static void eliminateDeadCode() {
        LOGGER.info("Eliminating dead code for IntroClassJava");
        DeadCode.clean("IntroClassJava");
    }
}
