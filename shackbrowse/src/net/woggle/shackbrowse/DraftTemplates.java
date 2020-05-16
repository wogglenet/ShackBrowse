package net.woggle.shackbrowse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;

public class DraftTemplates {
    protected String DRAFTSFILE_NAME = "savedtemplatesV1.cache";
    protected String FIRST_TOKEN = "<($#)*@)!@%$$%#!;[]_-;'d{{~`>";
    int MAX_DRAFTS_TO_SAVE = 30;
    private Activity _activity;

    Hashtable<String,String> mTpls = null;

    DraftTemplates (Activity activity) {
        _activity = activity;
        loadTplsFromDisk();
    }
    public void deleteTplById(String id)
    {
        if (mTpls != null)
        {
            mTpls.remove(id);
            saveTplsToDisk();
        }
    }
    public void saveThisTpl (String tplId, String TplText)
    {
        if (mTpls == null)
        {
            mTpls = new Hashtable<String,String>();
        }

        // trimming
        if (mTpls.size() > MAX_DRAFTS_TO_SAVE)
        {
            // trim down
            List<String> postIds = Collections.list(mTpls.keys());
            Collections.sort(postIds);

            List<String> postIdsToRemove = postIds.subList(0, postIds.size() - (1 + MAX_DRAFTS_TO_SAVE));
            for (String postId : postIdsToRemove)
            {
                mTpls.remove(postId);
            }
        }

        //if (truncatedParentPost.length() > 120)
        //	truncatedParentPost = truncatedParentPost.substring(0, 120);

        mTpls.put(tplId, TplText);
        saveTplsToDisk();
    }

    public void loadTplsFromDisk()
    {
        mTpls = new Hashtable<String,String>();
        if ((_activity != null) && (_activity.getFileStreamPath(DRAFTSFILE_NAME).exists()))
        {
            // look at that, we got a file
            try {
                FileInputStream input = _activity.openFileInput(DRAFTSFILE_NAME);
                try
                {
                    DataInputStream in = new DataInputStream(input);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = reader.readLine();
                    int i = 0;
                    while (line != null)
                    {
                        if (line.length() > 0)
                        {
                            if ((line.indexOf(FIRST_TOKEN) > -1) )
                            {
                                String[] draftbits = line.split(Pattern.quote(FIRST_TOKEN));
                                String tplName = draftbits[0];
                                String draft = "";
                                if (draftbits.length > 1)
                                    draft = draftbits[1];
                                i++;
                                mTpls.put(tplName, draft);
                            }
                        }
                        line = reader.readLine();
                    }
                } catch (NumberFormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                finally
                {
                    input.close();
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }
    public boolean saveTplsToDisk()
    {
        boolean result = true;
        try
        {
            FileOutputStream _output = _activity.openFileOutput(DRAFTSFILE_NAME, Activity.MODE_PRIVATE);
            try
            {
                DataOutputStream out = new DataOutputStream(_output);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
                // clear file
                writer.write("");
                int i = 0;

                Enumeration keys = mTpls.keys();
                System.out.println("TPL: size" + mTpls.size());
                while(keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    String value = mTpls.get(key);
                    i++;
                    // write line
                    System.out.println("TPL: " + key + FIRST_TOKEN + value);
                    writer.write(key + FIRST_TOKEN + value);
                    writer.newLine();
                }
                System.out.println("TPL: done" +i);
                writer.flush();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                result = false;
            }
            _output.close();
        }
        catch (Exception e)
        {
            result = false;
        }
        return result;
    }
}
