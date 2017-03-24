package com.shieldsbetter.scedel;

import java.util.LinkedList;
import java.util.List;
import com.shieldsbetter.scedel.values.Value;

public class InternalExecutionException extends RuntimeException {
    public static InternalExecutionException cannotBakeProxy(
            ParseLocation detected) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.CANNOT_BAKE_PROXY,
                        "Cannot bake value containing proxy object.",
                        detected, null));
    }
    
    public static InternalExecutionException illegalProxyContainment(
            ParseLocation detected) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.ILLEGAL_PROXY_CONTAINMENT,
                        "Proxy object cannot contain other proxy objects.",
                        detected, null));
    }
    
    public static InternalExecutionException divisionByZero(
            ParseLocation detected) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.DIVISION_BY_ZERO,
                        "Division by zero.", detected, null));
    }
    
    public static InternalExecutionException incorrectNumberOfParameters(
            ParseLocation detected, int expected, int got) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType
                                .INCORRECT_NUMBER_OF_PARAMETERS,
                        "Incorrect number of parameters.  Expected " + expected
                                + ", but got " + got + ".", detected, null));
    }
    
    public static InternalExecutionException notABoolean(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NOT_A_BOOLEAN,
                        "Value is not a boolean.", detected, offendingValue));
    }
    
    public static InternalExecutionException notADictionary(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NOT_A_DICTIONARY,
                        "Value is not a dictionary.", detected,
                        offendingValue));
    }
    
    public static InternalExecutionException notAFunction(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NOT_A_FUNCTION,
                        "Value is not a function.", detected, offendingValue));
    }
    
    public static InternalExecutionException notANumber(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NOT_A_NUMBER,
                        "Value is not a number.", detected, offendingValue));
    }
    
    public static InternalExecutionException notASequence(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NOT_A_SEQUENCE,
                        "Value is not a sequence.", detected, offendingValue));
    }
    
    public static InternalExecutionException notAString(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NOT_A_STRING,
                        "Value is not a string.", detected, offendingValue));
    }
    
    public static InternalExecutionException invalidWeighter(
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.INVALID_WEIGHTER,
                        "Weighter is not a dictionary or function.", detected,
                        offendingValue));
    }
    
    public static InternalExecutionException hostEnvironmentException(
            ParseLocation detected, String msg) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.HOST_ENVIRONMENT_EXCEPTION,
                        "Host environment exception: " + msg, detected, null));
    }
    
    public static InternalExecutionException nonIntegral(String what,
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NON_INTEGRAL,
                        "Non-integral " + what + ":" + offendingValue, detected,
                        offendingValue));
    }
    
    public static InternalExecutionException tooLarge(String what,
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.TOO_LARGE,
                        what + "too large: " + offendingValue, detected,
                        offendingValue));
    }
    
    public static InternalExecutionException negative(String what,
            ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType.NEGATIVE,
                        "Negative " + what + ": " + offendingValue, detected,
                        offendingValue));
    }
    
    public static InternalExecutionException
            plusFirstParamMustBeNumOrString(
                    ParseLocation detected, Value offendingValue) {
        return new InternalExecutionException(
                new ExecutionException(
                        ExecutionException.ErrorType
                                .PLUS_FIRST_PARAMETER_MUST_BE_NUMBER_OR_STRING,
                        "First parameter to plus must be a number or a string."
                                + "  Was: " + offendingValue, detected,
                        offendingValue));
    }
    
    private final ExecutionException myExecutionException;
    private final List<ParseLocation> myCallStack = new LinkedList<>();
    
    public InternalExecutionException(ExecutionException e) {
        myExecutionException = e;
    }
    
    public ExecutionException getExecutionException() {
        return myExecutionException.copy(myCallStack);
    }
    
    public void pushStackLevel(ParseLocation l) {
        myCallStack.add(l);
    }
}
