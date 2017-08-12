package biemann.android.eznearby;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

/**
 * Easy Nearby API library by Alexander Biemann
 * based on https://github.com/googlesamples/android-nearby/tree/master/messages/NearbyDevices
 */
public class EzNearby implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static final int TTL_IN_SECONDS = 3 * 60; // 3 minutes.
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder().setTtlSeconds(TTL_IN_SECONDS).build();
    private GoogleApiClient mGoogleApiClient;
    private MessageListener mMessageListener;
    private NearbyMessageListener mNearbyMessageListener;

    public interface NearbyMessageListener
    {
        void onDeviceFound(final String message);
        void onDeviceLost(final String message);
        void onConnectionFailed(@NonNull ConnectionResult connectionResult);
        void onConnectionSuspended(int i);
        void onConnected(@Nullable Bundle bundle);
        void onDeviceSubscription(boolean success, Status status);
        void onDeviceSubscriptionExpired();
        void onPublish(boolean success, Status status);
        void onPublishExpired();
    }

    /**
     * Constructor
     * @param listener required
     */
    public EzNearby(@NonNull NearbyMessageListener listener)
    {
        mNearbyMessageListener = listener;
        mMessageListener = new MessageListener()
        {
            @Override
            public void onFound(final Message message)
            {
                // Called when a new message is found.
                mNearbyMessageListener.onDeviceFound(DeviceMessage.fromNearbyMessage(message).getMessageBody());
            }

            @Override
            public void onLost(final Message message)
            {
                // Called when a message is no longer detectable nearby.
                mNearbyMessageListener.onDeviceLost(DeviceMessage.fromNearbyMessage(message).getMessageBody());
            }
        };
    }

    public boolean isConnected()
    {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    /**
     * must be called in onCreate()
     * @param activity will auto watch for onStart() and onStop() lifecycle events
     */
    public void buildGoogleApiClient(FragmentActivity activity)
    {
        if (mGoogleApiClient != null)
        {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(activity, this)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        mNearbyMessageListener.onConnected(bundle);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        mNearbyMessageListener.onConnectionSuspended(i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        mNearbyMessageListener.onConnectionFailed(connectionResult);
    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    public void subscribe()
    {
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback()
                {
                    @Override
                    public void onExpired()
                    {
                        super.onExpired();
                        mNearbyMessageListener.onDeviceSubscriptionExpired();
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>()
                {
                    @Override
                    public void onResult(@NonNull Status status)
                    {
                        if (status.isSuccess())
                        {
                            mNearbyMessageListener.onDeviceSubscription(true, status);
                        }
                        else
                        {
                            mNearbyMessageListener.onDeviceSubscription(false, status);
                        }
                    }
                });
    }

    /**
     * Stops subscribing to messages from nearby devices.
     */
    public void unsubscribe()
    {
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    public void publish(Message message)
    {
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback()
                {
                    @Override
                    public void onExpired()
                    {
                        super.onExpired();
                        mNearbyMessageListener.onPublishExpired();
                    }
                }).build();

        Nearby.Messages.publish(mGoogleApiClient, message, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess())
                        {
                            mNearbyMessageListener.onPublish(true, status);
                        }
                        else
                        {
                            mNearbyMessageListener.onPublish(false, status);
                        }
                    }
                });
    }

    /**
     * Stops publishing message to nearby devices.
     */
    public void unpublish(Message pubMessage)
    {
        Nearby.Messages.unpublish(mGoogleApiClient, pubMessage);
    }

}
