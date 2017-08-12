package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Messages;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

import biemann.android.eznearby.DeviceMessage;
import biemann.android.eznearby.EzNearby;

/**
 * An activity that allows a user to publish device information, and receive information about
 * nearby devices.
 * <p/>
 * The UI exposes a button to subscribe to broadcasts from nearby devices, and another button to
 * publish messages that can be read nearby subscribing devices. Both buttons toggle state,
 * allowing the user to cancel a subscription or stop publishing.
 * <p/>
 * This activity demonstrates the use of the
 * {@link Messages#subscribe(GoogleApiClient, MessageListener, SubscribeOptions)},
 * {@link Messages#unsubscribe(GoogleApiClient, MessageListener)},
 * {@link Messages#publish(GoogleApiClient, Message, PublishOptions)}, and
 * {@link Messages#unpublish(GoogleApiClient, Message)} for foreground publication and subscription.
 * <p/>a
 * We check the app's permissions and present an opt-in dialog to the user, who can then grant the
 * required location permission.
 * <p/>
 * Using Nearby for in the foreground is battery intensive, and pub-sub is best done for short
 * durations. In this sample, we set the TTL for publishing and subscribing to three minutes
 * using a {@link Strategy}. When the TTL is reached, a publication or subscription expires.
 */
public class MainActivity extends AppCompatActivity implements EzNearby.NearbyMessageListener
{
    // Constants
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_UUID = "key_uuid"; // Key used in writing to and reading from SharedPreferences.

    // Views.
    private SwitchCompat mPublishSwitch;
    private SwitchCompat mSubscribeSwitch;

    // Members
    private EzNearby mEzNearby;
    private Message mPubMessage; //object used to broadcast information
    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;

    /**
     * Creates a UUID and saves it to {@link SharedPreferences}. The UUID is added to the published
     * message to avoid it being undelivered due to de-duplication. See {@link DeviceMessage} for
     * details.
     */
    private static String getUUID(SharedPreferences sharedPreferences) {
        String uuid = sharedPreferences.getString(KEY_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSubscribeSwitch = (SwitchCompat) findViewById(R.id.subscribe_switch);
        mPublishSwitch = (SwitchCompat) findViewById(R.id.publish_switch);

        // Build the message that is going to be published. This contains the device name and a UUID
        mPubMessage = DeviceMessage.newNearbyMessage(
                getUUID(getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE)));

        mEzNearby = new EzNearby(this);


        mSubscribeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // If GoogleApiClient is connected, perform sub actions in response to user action.
                // If it isn't connected, do nothing, and perform sub actions when it connects (see
                // onConnected()).
                if (mEzNearby.isConnected()) {
                    if (isChecked) {
                        mEzNearby.subscribe();
                    } else {
                        mEzNearby.unsubscribe();
                    }
                }
            }
        });

        mPublishSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // If GoogleApiClient is connected, perform pub actions in response to user action.
                // If it isn't connected, do nothing, and perform pub actions when it connects (see
                // onConnected()).
                if (mEzNearby.isConnected()) {
                    if (isChecked) {
                        mEzNearby.publish(mPubMessage);
                    } else {
                        mEzNearby.unpublish(mPubMessage);
                    }
                }
            }
        });

        final List<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);

        final ListView nearbyDevicesListView = (ListView) findViewById(R.id.nearby_devices_list_view);
        if (nearbyDevicesListView != null) {
            nearbyDevicesListView.setAdapter(mNearbyDevicesArrayAdapter);
        }

        mEzNearby.buildGoogleApiClient(this);
    }


    @Override
    public void onDeviceFound(String message) {
        // Called when a new message is found.
        mNearbyDevicesArrayAdapter.add(message);
    }

    @Override
    public void onDeviceLost(String message) {
        mNearbyDevicesArrayAdapter.remove(message);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mPublishSwitch.setEnabled(false);
        mSubscribeSwitch.setEnabled(false);
        logAndShowSnackbar("Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onConnectionSuspended(int i) {
        logAndShowSnackbar("Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // We use the Switch buttons in the UI to track whether we were previously doing pub/sub (
        // switch buttons retain state on orientation change). Since the GoogleApiClient disconnects
        // when the activity is destroyed, foreground pubs/subs do not survive device rotation. Once
        // this activity is re-created and GoogleApiClient connects, we check the UI and pub/sub
        // again if necessary.
        if (mPublishSwitch.isChecked()) {
            mEzNearby.publish(mPubMessage);
        }
        if (mSubscribeSwitch.isChecked()) {
            mEzNearby.subscribe();
        }
    }

    @Override
    public void onDeviceSubscription(boolean success, Status status) {
        if (success)
        {
            Log.i(TAG, "Subscribed successfully.");
        }
        else
        {
            logAndShowSnackbar("Could not subscribe, status = " + status);
            mSubscribeSwitch.setChecked(false);
        }
    }

    @Override
    public void onDeviceSubscriptionExpired() {
        Log.i(TAG, "No longer subscribing");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSubscribeSwitch.setChecked(false);
            }
        });
    }

    @Override
    public void onPublishExpired() {
        Log.i(TAG, "No longer publishing");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPublishSwitch.setChecked(false);
            }
        });
    }

    @Override
    public void onPublish(boolean success, Status status) {
        if (success)
        {
            Log.i(TAG, "Published successfully.");
        }
        else
        {
            logAndShowSnackbar("Could not publish, status = " + status);
            mPublishSwitch.setChecked(false);
        }
    }

    /**
     * Logs a message and shows a {@link Snackbar} using {@code text};
     *
     * @param text The text used in the Log message and the SnackBar.
     */
    private void logAndShowSnackbar(final String text) {
        Log.w(TAG, text);
        View container = findViewById(R.id.activity_main_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}