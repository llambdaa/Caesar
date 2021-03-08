package engine.caesar.arg;

public abstract class Argument {

    protected boolean essential;
    protected int index;

    public Argument( boolean essential, int index ) {

        this.essential = essential;
        this.index = index;

    }

    public boolean isEssential() {

        return this.essential;

    }

    public int getIndex() {

        return this.index;

    }

}
