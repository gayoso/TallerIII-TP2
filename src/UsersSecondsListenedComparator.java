import java.util.Comparator;


public class UsersSecondsListenedComparator
        implements Comparator<UserSecondsListened> {
    @Override
    public int compare(UserSecondsListened o1, UserSecondsListened o2) {

        return (int)Math.signum(o2.getSecondsListened() -
                o1.getSecondsListened());
    }
}