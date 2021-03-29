package engine.caesar.exception.parse;

public class GroupMissingSchemeException extends Exception {

    public GroupMissingSchemeException( String identifier ) {

        super( String.format( "Group '%s' is missing any scheme definition.", identifier ) );

    }

}
