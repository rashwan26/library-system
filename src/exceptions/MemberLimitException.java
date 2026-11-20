package exceptions;

public class MemberLimitException extends LibraryException {
    private int currentCount;
    private int maxLimit;

    public MemberLimitException(int currentCount, int maxLimit) {
        super("Borrowing limit exceeded. Current: " + currentCount + ", Maximum: " + maxLimit,
              "MEMBER-001");
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxLimit() {
        return maxLimit;
    }
}
