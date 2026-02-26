package ai.narrativetrace.benchmarks.services;

import ai.narrativetrace.benchmarks.PlainService;

public class PlainServiceImpl implements PlainService {
    @Override
    public String execute(String input) {
        return "result:" + input;
    }
}
