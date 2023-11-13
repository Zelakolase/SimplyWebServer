package sws.utils;

public class Flag {
    private boolean flag;

    public Flag() {
        this(true);
    }

    public Flag(boolean flag) {
        this.flag = flag;
    }

    public boolean isSet() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
