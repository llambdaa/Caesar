package engine.caesar.exception.parse;

public class GroupTooFewValuesException extends Exception {

    public GroupTooFewValuesException( String identifier, int got, int expected ) {

        super( String.format( "Group '%s' got only %s values but expected %s.", identifier, got, expected ) );

    }

}
