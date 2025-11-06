import java.util.Random;

public class Quickselect {

    public static ServerStatus quickselect(ServerStatus[] arr, int k) {
        if (k < 0 || k >= arr.length) {
            throw new IllegalArgumentException("k est hors des limites du tableau");
        }
        return quickselect(arr, 0, arr.length - 1, k);
    }

    private static ServerStatus quickselect(ServerStatus[] arr, int left, int right, int k) {
        if (left == right) {
            return arr[left];
        }

        Random random = new Random();
        int pivotIndex = left + random.nextInt(right - left + 1);
        pivotIndex = partition(arr, left, right, pivotIndex);

        if (k == pivotIndex) {
            return arr[k];
        } else if (k < pivotIndex) {
            return quickselect(arr, left, pivotIndex - 1, k);
        } else {
            return quickselect(arr, pivotIndex + 1, right, k);
        }
    }

    private static int partition(ServerStatus[] arr, int left, int right, int pivotIndex) {
        long pivotValue = arr[pivotIndex].freeSpace;
        swap(arr, pivotIndex, right);
        int storeIndex = left;

        for (int i = left; i < right; i++) {
            if (arr[i].freeSpace < pivotValue) {
                swap(arr, storeIndex, i);
                storeIndex++;
            }
        }
        swap(arr, right, storeIndex);
        return storeIndex;
    }

    private static void swap(ServerStatus[] arr, int i, int j) {
        ServerStatus temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

}
