package Task2;

public class Task2 {

    private int counter = 1;
    private boolean increment = true;
    private int turn = 1;

    public synchronized void printAndUpdate(int id, int N) {
        while (turn != id) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Thread " + id + ": " + counter);

        if (increment) {
            if (counter < N) {
                counter++;
            } else {
                increment = false;
                counter--;
            }
        } else {
            if (counter > 1) {
                counter--;
            } else {
                // back to 1 → terminate
                notifyAll();
                return;
            }
        }

        if (id == 1) {
            turn = 2;
        } else if (id == 2) {
            turn = 3;
        } else {
            turn = 1;
        }

        notifyAll();
    }

    public static void main(String[] args) {
        System.out.print("Enter a maximum number N: ");
        int N = Integer.parseInt(System.console().readLine());
        Task2 syncCounter = new Task2();

        Runnable task1 = () -> {
            while (true) {
                syncCounter.printAndUpdate(1, N);
                if (syncCounter.counter == 1 && !syncCounter.increment) {
                    break;
                }
            }
        };

        Runnable task2 = () -> {
            while (true) {
                syncCounter.printAndUpdate(2, N);
                if (syncCounter.counter == 1 && !syncCounter.increment) {
                    break;
                }
            }
        };

        Runnable task3 = () -> {
            while (true) {
                syncCounter.printAndUpdate(3, N);
                if (syncCounter.counter == 1 && !syncCounter.increment) {
                    break;
                }
            }
        };

        Thread.ofVirtual().start(task1);
        Thread.ofVirtual().start(task2);
        Thread.ofVirtual().start(task3);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
    }
}
