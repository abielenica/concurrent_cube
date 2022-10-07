package concurrentcube;

public class Monitor {
    private GroupType activeGroup = GroupType.NONE;
    private int activeProcesses = 0;
    private final boolean[] rotatedLayers;

    public Monitor(int size) {
        rotatedLayers = new boolean[size];
    }

    public synchronized void syncGroupsBefore(GroupType myGroup) throws InterruptedException {
        while (this.activeGroup != myGroup && this.activeGroup != GroupType.NONE) {
            wait();
        }
        activeProcesses++;
        this.activeGroup = myGroup;
    }

    public synchronized void syncGroupsAfter() {
        activeProcesses--;
        if (activeProcesses == 0) {
            this.activeGroup = GroupType.NONE;
            notifyAll();
        }
    }

    public synchronized void syncBeforeRotate(int layer) throws InterruptedException {
        while (rotatedLayers[layer]) {
            try {
                wait();
            } catch (InterruptedException e) {
                activeProcesses--;
                if (activeProcesses == 0) {
                    activeGroup = GroupType.NONE;
                    notifyAll();
                }
                throw new InterruptedException();
            }
        }
        rotatedLayers[layer] = true;
    }

    public synchronized void syncAfterRotate(int layer) {
        rotatedLayers[layer] = false;
        notifyAll();
    }

    public int getStandardisedLayer(int side, int layer, int size) {
        switch (side) {
            case 0:
            case 1:
            case 2:
                return layer;
            default:
                return size - 1 - layer;
        }
    }

    public GroupType getRotateGroupType(int layer) {
        switch (layer) {
            case 0:
            case 5:
                return GroupType.ROTATE_Z;
            case 1:
            case 3:
                return GroupType.ROTATE_X;
            default:
                return GroupType.ROTATE_Y;
        }
    }
}
