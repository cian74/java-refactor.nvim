package com.cian.refactor;

import com.cian.refactor.profiler.Profiler;
import com.cian.refactor.strategy.RefactoringStrategy;
import com.cian.refactor.strategy.RefactoringStrategyFactory;
import com.google.gson.Gson;

public class RefactoringEngine {
    private static final Gson gson = new Gson();
    
    public String applyRefactor(String command, Request request) {
        if (command.equals("profile_method")) {
            Profiler profiler = new Profiler();
            Refactored result = profiler.profileMethod(
                request.source,
                request.class_name,
                request.method_name,
                request.start_line
            );
            return gson.toJson(result);
        }
        
        return RefactoringStrategyFactory.getStrategy(command)
            .map(strategy -> executeStrategy(strategy, command, request))
            .orElseGet(() -> {
                Refactored result = new Refactored();
                result.error = "Unknown command: " + command;
                return gson.toJson(result);
            });
    }
    
    private String executeStrategy(RefactoringStrategy strategy, String command, Request request) {
        Refactored result;
        
        switch (command) {
            case "extract_method":
                result = strategy.apply(
                    request.source,
                    request.start_line,
                    request.end_line,
                    request.highlighted,
                    request.method_name
                );
                break;
                
            case "extract_variable":
                result = strategy.apply(
                    request.source,
                    request.highlighted,
                    request.var_name,
                    request.start_line
                );
                break;
                
            case "inline_method":
                result = strategy.apply(
                    request.source,
                    request.start_line
                );
                break;
                
            case "generate_field_getters_setters":
                result = strategy.apply(
                    request.source,
                    request.selected_fields
                );
                break;
                
            case "rename":
                result = strategy.apply(
                    request.source,
                    request.old_name,
                    request.new_name,
                    request.start_line,
                    request.scope
                );
                break;
                
            case "encapsulate_field":
                result = strategy.apply(
                    request.source,
                    request.field_name
                );
                break;
                
case "pull_push":
                result = strategy.apply(
                    request.source,
                    request.direction,
                    request.member_name,
                    request.start_line
                );
                break;
            
            case "extract_interface":
                result = strategy.apply(
                    request.source,
                    request.interface_name,
                    request.method_names
                );
                break;
                
                case "generate_getters_setters":
            case "list_fields":
            case "generate_toString":
            default:
                result = strategy.apply(request.source);
                break;
        }
        
        if (result.new_source != null) {
            int lineCount = result.new_source.split("\n").length;
            result.new_source_lines = lineCount;
            result.needs_confirmation = lineCount > 100;
        }
        
        return gson.toJson(result);
    }
    
    public String capitalise(String methodName) {
        if (methodName == null || methodName.isEmpty()) return methodName;
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }
}
