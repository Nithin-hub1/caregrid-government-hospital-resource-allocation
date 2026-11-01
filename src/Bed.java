public class Bed {

    private String bedId;
    private String type;
    private boolean available;

    public Bed(String bedId, String type, boolean available) {

        this.bedId = bedId;
        this.type = type;
        this.available = available;
    }

    public String getBedId() {
        return bedId;
    }

    public String getType() {
        return type;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {

        return "Bed ID: " + bedId +
                " | Type: " + type +
                " | Available: " + available;
    }
}