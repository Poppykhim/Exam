package Task1;

public class Task1 {

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
                notifyAll();
                return;
            }
        }
        turn = (id == 1) ? 2 : 1;
        notifyAll();
    }

    public static void main(String[] args) {

        System.out.print("Enter a maximum number N: ");
        int N = Integer.parseInt(System.console().readLine());
        Task1 syncCounter = new Task1();

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

        Thread.ofVirtual().start(task1);
        Thread.ofVirtual().start(task2);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }
}
