package ai.narrativetrace.core.output;

import java.nio.file.Path;

public final class OutputDirectoryResolver {

    private final Path baseDir;

    public OutputDirectoryResolver(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Path traceDirectory(String testClassName) {
        var simpleName = testClassName.contains(".")
                ? testClassName.substring(testClassName.lastIndexOf('.') + 1)
                : testClassName;
        return baseDir.resolve("traces").resolve(simpleName);
    }

    public Path traceFile(String testClassName, String testMethodName) {
        return traceDirectory(testClassName).resolve(toFileSlug(testMethodName) + ".md");
    }

    public Path baseDir() {
        return baseDir;
    }

    private String toFileSlug(String methodName) {
        return methodName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
