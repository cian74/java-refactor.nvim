package com.cian.refactor.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RefactoringStrategyFactory {
    
    private static final Map<String, RefactoringStrategy> strategies = new HashMap<>();
    
    static {
        strategies.put("generate_getters_setters", new GenerateGettersSetters());
        strategies.put("extract_method", new ExtractMethodStrategy());
        strategies.put("list_fields", new ListFieldsStrategy());
        strategies.put("generate_field_getters_setters", new GenerateFieldGettersSettersStrategy());
        strategies.put("inline_method", new InlineMethodStrategy());
        strategies.put("generate_toString", new GenerateToStringStrategy());
        strategies.put("extract_variable", new ExtractVariableStrategy());
    }
    
    public static Optional<RefactoringStrategy> getStrategy(String command) {
        return Optional.ofNullable(strategies.get(command));
    }
    
    public static boolean hasStrategy(String command) {
        return strategies.containsKey(command);
    }
    
    public static Iterable<String> getAvailableCommands() {
        return strategies.keySet();
    }
}
