package com.prim.breadcrumbs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.actions.tasks.XmlCreator;
import dev.ugasoft.android.gps.actions.utils.ProgressListener;
import dev.ugasoft.android.gps.adapter.BreadcrumbsAdapter;
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
public class GetBreadcrumbsBundlesTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsBundlesTask";
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpclient;
   
   private Set<Integer> mBundleIds;
   private LinkedList<Object[]> mBundles;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient
    * @param listener
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsBundlesTask(Context context, BreadcrumbsService adapter, ProgressListener listener, DefaultHttpClient httpclient, OAuthConsumer consumer)
   {
      super(context, adapter, listener);
      mHttpclient = httpclient;
      mConsumer = consumer;

   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Void doInBackground(Void... params)
   {
      HttpEntity responseEntity = null;
      mBundleIds = new HashSet<Integer>();
      mBundles = new LinkedList<Object[]>();
      try
      {
         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/bundles.xml");
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
         HttpResponse response = mHttpclient.execute(request);
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

         String bundleName = null, bundleDescription = null;
         Integer bundleId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("bundle".equals(xpp.getName()) && bundleId != null)
               {
                  mBundles.add( new Object[]{bundleId, bundleName, bundleDescription} );
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("description".equals(tagName))
               {
                  bundleDescription = xpp.getText();
               }
               else if ("id".equals(tagName))
               {
                  bundleId = Integer.parseInt(xpp.getText());
                  mBundleIds.add(bundleId);
               }
               else if ("name".equals(tagName))
               {
                  bundleName = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (OAuthMessageSignerException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "The authentication communication failed");
      }
      catch (IOException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem during communication");
      }
      catch (XmlPullParserException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem while reading the XML data");
      }
      catch (IllegalStateException e) 
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem during communication");
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
               Log.w(TAG, "Failed closing inputstream");
            }
         }
      }
      return null;
   }

   @Override
   protected void updateTracksData(BreadcrumbsTracks tracks)
   {
      tracks.setAllBundleIds( mBundleIds );
      
      for( Object[] bundle : mBundles )
      {
         Integer bundleId = (Integer) bundle[0];
         String bundleName = (String) bundle[1];
         String bundleDescription = (String) bundle[2];
         
         tracks.addBundle(bundleId, bundleName, bundleDescription);
      }
   }
}