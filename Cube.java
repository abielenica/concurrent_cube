package concurrentcube;

import concurrentcube.Monitor;
import concurrentcube.GroupType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;

public class Cube {
    private final int size;
    private final ArrayList<ArrayList<ArrayList<Integer>>> sides;
    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;
    private final int[] oppositeSide = {5, 3, 4, 1, 2, 0};
    private final Monitor monitor;

    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing
    ) {

        this.size = size;
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;
        this.sides = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sides.add(new ArrayList<>(size));
            for (int j = 0; j < size; j++) {
                sides.get(i).add(new ArrayList<>(size));
            }
        }

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    sides.get(i).get(j).add(i);
                }
            }
        }
        this.monitor = new Monitor(size);
    }

    public int getSize() {
        return size;
    }

    private ArrayList<Integer> getRow(int side, int number) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int c = 0; c < size; c++) {
            result.add(sides.get(side).get(c).get(number));
        }
        return result;
    }

    private ArrayList<Integer> getReversedRow(int side, int number) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int c = size - 1; c >= 0; c--) {
            result.add(sides.get(side).get(c).get(number));
        }
        return result;
    }

    public ArrayList<Integer> getColumn(int side, int number) {
        return sides.get(side).get(number);
    }

    private ArrayList<Integer> getReversedColumn(int side, int number) {
        ArrayList<Integer> result = sides.get(side).get(number);
        Collections.reverse(result);
        return result;
    }

    private void replaceValue(int side, int a, int b, int c, int d) {
        sides.get(side).get(a).set(b, sides.get(side).get(c).get(d));
    }

    private void rotateSideTopLayer(int side) throws InterruptedException {
        for (int i = 0; i < (size + 1) / 2; i++) {
            for (int j = 0; j < size / 2; j++) {
                int temp = sides.get(side).get(i).get(size - 1 - j);
                replaceValue(side, i, size - 1 - j, size - j - 1, size - 1 - i);
                replaceValue(side, size - j - 1, size - 1 - i, size - 1 - i, j);
                replaceValue(side, size - 1 - i, j, j, i);
                sides.get(side).get(j).set(i, temp);
            }
        }
    }

    private void rotateSideTopLayerAntiClockwise(int side) throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            rotateSideTopLayer(side);
        }
    }


    private void replaceRow(int oldRowSide, int layer, ArrayList<Integer> newRow) throws InterruptedException {
        for (int i = 0; i < size; i++) {
            ((sides.get(oldRowSide)).get(i)).set(layer, newRow.get(i));
        }
    }

    private void replaceColumn(int oldSide, int colNum, ArrayList<Integer> newColumn) throws InterruptedException {
        sides.get(oldSide).set(colNum, newColumn);
    }

    private void rotateTop(int layer) throws InterruptedException {
        ArrayList<Integer> tempRow = getRow(1, layer);
        for (int i = 1; i < 4; i++) {
            replaceRow(i, layer, getRow(i + 1, layer));
        }
        replaceRow(4, layer, tempRow);
    }

    private void rotateBottom(int layer) throws InterruptedException {
        layer = size - 1 - layer;
        ArrayList<Integer> tempRow = getRow(4, layer);
        for (int i = 4; i > 1; i--) {
            ArrayList<Integer> newRow = getRow(i - 1, layer);
            replaceRow(i, layer, newRow);
        }
        replaceRow(1, layer, tempRow);
    }

    private void rotateFront(int layer) throws InterruptedException {
        int complement = size - 1 - layer;
        ArrayList<Integer> tempColumn = getRow(0, complement);
        replaceRow(0, complement, getReversedColumn(1, complement));
        replaceColumn(1, complement, getRow(5, layer));
        replaceRow(5, layer, getReversedColumn(3, layer));
        replaceColumn(3, layer, tempColumn);
    }

    private void rotateBack(int layer) throws InterruptedException {
        int complement = size - 1 - layer;
        ArrayList<Integer> tempColumn = getReversedRow(0, layer);
        replaceRow(0, layer, getColumn(3, complement));
        replaceColumn(3, complement, getReversedRow(5, complement));
        replaceRow(5, complement, getColumn(1, layer));
        replaceColumn(1, layer, tempColumn);
    }

    private void rotateLeft(int layer) throws InterruptedException {
        int complement = size - 1 - layer;
        ArrayList<Integer> tempColumn = getColumn(0, layer);
        replaceColumn(0, layer, getReversedColumn(4, complement));
        replaceColumn(4, complement, getReversedColumn(5, layer));
        replaceColumn(5, layer, getColumn(2, layer));
        replaceColumn(2, layer, tempColumn);
    }

    private void rotateRight(int layer) throws InterruptedException {
        int complement = size - 1 - layer;
        ArrayList<Integer> tempColumn = getReversedColumn(0, complement);
        replaceColumn(0, complement, getColumn(2, complement));
        replaceColumn(2, complement, getColumn(5, complement));
        replaceColumn(5, complement, getReversedColumn(4, layer));
        replaceColumn(4, layer, tempColumn);
    }


    public void rotate(int side, int layer) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        int standardisedLayer = monitor.getStandardisedLayer(side, layer, size);
        monitor.syncGroupsBefore(monitor.getRotateGroupType(side));
        monitor.syncBeforeRotate(standardisedLayer);
        beforeRotation.accept(side, layer);
        if (layer == 0)
            rotateSideTopLayer(side);
        else if (layer == size - 1)
            rotateSideTopLayerAntiClockwise(oppositeSide[side]);
        switch (side) {
            case 0:
                rotateTop(layer);
                break;
            case 1:
                rotateLeft(layer);
                break;
            case 2:
                rotateFront(layer);
                break;
            case 3:
                rotateRight(layer);
                break;
            case 4:
                rotateBack(layer);
                break;
            default:
                rotateBottom(layer);
        }
        afterRotation.accept(side, layer);
        monitor.syncAfterRotate(standardisedLayer);
        monitor.syncGroupsAfter();
    }

    public String show() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        monitor.syncGroupsBefore(GroupType.SHOW);
        beforeShowing.run();
        StringBuilder cubeView = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    cubeView.append(sides.get(i).get(c).get(r).toString());
                }
            }
        }
        afterShowing.run();
        monitor.syncGroupsAfter();
        return cubeView.toString();
    }
}