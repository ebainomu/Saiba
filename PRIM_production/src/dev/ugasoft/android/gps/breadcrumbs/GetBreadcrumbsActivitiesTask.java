package dev.ugasoft.android.gps.breadcrumbs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.actions.tasks.XmlCreator;
import dev.ugasoft.android.gps.actions.utils.ProgressListener;
import dev.ugasoft.android.gps.adapter.BreadcrumbsAdapter;
import dev.ugasoft.android.gps.util.Pair;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.client.methods.HttpUriRequest;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class GetBreadcrumbsActivitiesTask extends BreadcrumbsTask
{

   private LinkedList<Pair<Integer, String>> mActivities;
   final String TAG = "OGT.GetBreadcrumbsActivitiesTask";
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpClient;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsActivitiesTask(Context context, BreadcrumbsService adapter, ProgressListener listener, DefaultHttpClient httpclient, OAuthConsumer consumer)
   {
      super(context, adapter, listener);
      mHttpClient = httpclient;
      mConsumer = consumer;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Void doInBackground(Void... params)
   {
      mActivities = new LinkedList<Pair<Integer,String>>(); 
      HttpEntity responseEntity = null;
      try
      {
         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/activities.xml");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         mConsumer.sign(request);
         if( BreadcrumbsAdapter.DEBUG )
         {
            Log.d( TAG, "Execute request: "+request.getURI() );
            for( Header header : request.getAllHeaders() )
            {
               Log.d( TAG, "   with header: "+header.toString());
            }
         }
         HttpResponse response = mHttpClient.execute(request);
         responseEntity = response.getEntity();
         InputStream is = responseEntity.getContent();
         InputStream stream = new BufferedInputStream(is, 8192);
         if( BreadcrumbsAdapter.DEBUG )
         {
            stream = XmlCreator.convertStreamToLoggedStream(TAG, stream);
         }
         
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         XmlPullParser xpp = factory.newPullParser();
         xpp.setInput(stream, "UTF-8");

         String tagName = null;
         int eventType = xpp.getEventType();

         String activityName = null;
         Integer activityId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("activity".equals(xpp.getName()) && activityId != null && activityName != null)
               {
                  mActivities.add(new Pair<Integer, String>(activityId, activityName));
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("id".equals(tagName))
               {
                  activityId = Integer.parseInt(xpp.getText());
               }
               else if ("name".equals(tagName))
               {
                  activityName = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (OAuthMessageSignerException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "The authentication communication failed");
      }
      catch (IOException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "A problem during communication");
      }
      catch (XmlPullParserException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "A problem while reading the XML data");
      }
      finally
      {
         if (responseEntity != null)
         {
            try
            {
               EntityUtils.consume(responseEntity);
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close the content stream", e);
            }
         }
      }
      return null;
   }
   
   @Override
   protected void updateTracksData( BreadcrumbsTracks tracks )
   {
      for( Pair<Integer, String> activity : mActivities )
      {
        tracks.addActivity(activity.first, activity.second);
      }
   }
}