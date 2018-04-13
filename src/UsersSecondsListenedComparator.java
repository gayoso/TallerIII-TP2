import java.util.Comparator;


public class UsersSecondsListenedComparator
        implements Comparator<UserSecondsListened> {
    @Override
    public int compare(UserSecondsListened o1, UserSecondsListened o2) {

        if (o2.secondsListened < o1.secondsListened) {
            return -1;
        }
        if (o2.secondsListened > o1.secondsListened) {
            return 1;
        }
        return 0;
    }
}