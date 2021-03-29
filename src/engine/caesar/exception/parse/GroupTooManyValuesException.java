package engine.caesar.exception.parse;

public class GroupTooManyValuesException extends Exception {

    public GroupTooManyValuesException(String identifier, int got, int expected ) {

        super( String.format( "Group '%s' got %s values but only %s.", identifier, got, expected ) );

    }

}
