package org.visallo.tools.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public abstract class Args {
    @Parameter(names = {"--help", "-h"}, description = "Print help", help = true)
    public boolean help;

    public void validate(JCommander j) {

    }
}
