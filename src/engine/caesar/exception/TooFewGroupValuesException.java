package engine.caesar.exception;

public class TooFewGroupValuesException extends Exception {

    public TooFewGroupValuesException(String identifier, int got, int expected ) {

        super( String.format( "Argument '%s' got %s values but only expected %s.", identifier, got, expected ) );

    }

}
