package ai.narrativetrace.agent;

record MethodMetadata(
    String[] parameterNames, boolean[] redacted, String narratedTemplate, OnErrorEntry[] onErrors) {

  record OnErrorEntry(String template, String exceptionDescriptor) {}
}
