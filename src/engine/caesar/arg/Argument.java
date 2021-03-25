package engine.caesar.arg;

public abstract class Argument {

    protected boolean essential;

    public Argument( boolean essential ) {

        this.essential = essential;

    }

    public boolean isEssential() {

        return this.essential;

    }

    public void setEssential( boolean essential ) {

        this.essential = essential;

    }

}
