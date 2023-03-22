package net.swigglesoft.shackbrowse.twitter;

import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.models.MediaEntity;
import com.twitter.sdk.android.core.models.Tweet;

import java.lang.reflect.Field;

public class TweetDataFixer {
    public static Tweet fixTweetMediaData(Result<Tweet> tweet) throws NoSuchFieldException, IllegalAccessException {
        // Sometimes tweets come in with null video data, if that's the case we hack it with Reflection
        // to be a photo type :( Otherwise this crashes the Twitter SDK which crashes the app.
        for(MediaEntity media : tweet.data.entities.media) {
            fixTypeField(media);
        }
        for(MediaEntity media : tweet.data.extendedEntities.media) {
            fixTypeField(media);
        }
        return tweet.data;
    }
    private static void fixTypeField(MediaEntity media) throws NoSuchFieldException, IllegalAccessException {
        if((media.type.equalsIgnoreCase("video") || media.type.equalsIgnoreCase("animated_gif"))
                && media.videoInfo == null) {
            Field f = media.getClass().getDeclaredField("type");
            f.setAccessible(true);
            f.set(media, "photo");
        }
    }
}
