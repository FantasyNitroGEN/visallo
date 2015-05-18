package org.visallo.tools;

import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.ServiceLoaderUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class Cli extends CommandLineTool {
    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new Cli(), args, false);
    }

    @Override
    public int run(String[] args, boolean initFramework) throws Exception {
        List<Class<? extends CommandLineTool>> commandLineToolClasses = toList(findCommandLineToolClasses());

        if (args.length == 0) {
            printHelp(commandLineToolClasses, "Require tools classname");
            return -1;
        }

        String className = args[0];
        String[] remainingOfArgs = Arrays.copyOfRange(args, 1, args.length);

        Class<? extends CommandLineTool> clazz = findToolClass(commandLineToolClasses, className);
        if (clazz == null) {
            return -1;
        }
        Method mainMethod = clazz.getMethod("main", String[].class);
        mainMethod.invoke(null, new Object[]{remainingOfArgs});
        return 0;
    }

    private void printHelp(List<Class<? extends CommandLineTool>> commandLineToolClasses, String message) {
        System.err.println(message);

        Collections.sort(commandLineToolClasses, new Comparator<Class<? extends CommandLineTool>>() {
            @Override
            public int compare(Class<? extends CommandLineTool> o1, Class<? extends CommandLineTool> o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });

        int maxLength = Ordering.natural().max(Iterables.transform(commandLineToolClasses, new Function<Class<? extends CommandLineTool>, Integer>() {
            @Nullable
            @Override
            public Integer apply(Class<? extends CommandLineTool> input) {
                return input.getSimpleName().length();
            }
        }));

        for (Class<? extends CommandLineTool> commandLineToolClass : commandLineToolClasses) {
            String description = getDescription(commandLineToolClass);
            System.err.println(String.format("  %-" + maxLength + "s  %s", commandLineToolClass.getSimpleName(), description));
        }
    }

    private String getDescription(Class<? extends CommandLineTool> commandLineToolClass) {
        Parameters parameters = commandLineToolClass.getAnnotation(Parameters.class);
        if (parameters == null) {
            return "";
        }
        return parameters.commandDescription();
    }

    @Override
    protected int run() throws Exception {
        throw new VisalloException("This run should not be called.");
    }

    private Iterable<Class<? extends CommandLineTool>> findCommandLineToolClasses() {
        return ServiceLoaderUtil.loadClasses(CommandLineTool.class, getConfiguration());
    }

    private Class<? extends CommandLineTool> findToolClass(List<Class<? extends CommandLineTool>> commandLineToolClasses, String classname) throws ClassNotFoundException {
        for (Class<? extends CommandLineTool> commandLineToolClass : commandLineToolClasses) {
            if (commandLineToolClass.getName().equalsIgnoreCase(classname) || commandLineToolClass.getSimpleName().equalsIgnoreCase(classname)) {
                return commandLineToolClass;
            }
        }
        printHelp(commandLineToolClasses, "Could not find command line tool: " + classname);
        return null;
    }
}
