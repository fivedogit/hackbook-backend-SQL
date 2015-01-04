package club.hackbook.domain;

import java.util.Comparator;

public class FollowerCountComparator implements Comparator<User>
{
    public int compare(User x, User y)
    {
        // Assume neither string is null. Real code should
        // probably be more robust
        // You could also just return x.length() - y.length(),
        // which would be more efficient.
    	if(x.getFollowers() == null)
    		return -1;
    	if(y.getFollowers() == null) // obviously x.getFollowers() != null at this point 
    		return 1;
        if (x.getFollowers().size() < y.getFollowers().size())
            return -1;
        if (x.getFollowers().size() > y.getFollowers().size())
            return 1;
        return 0;
    }
}