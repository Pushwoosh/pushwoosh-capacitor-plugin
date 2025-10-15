import { Pushwoosh } from 'pushwoosh-capacitor-plugin';

function showMessage(text) {
    const container = document.getElementById('messageArea');
    const message = document.createElement('p');
    message.textContent = text;
    container.appendChild(message);
}


window.testEcho = async () => {
    const inputValue = document.getElementById("echoInput").value;
    const pushReceivedCallbackId = await Pushwoosh.pushReceivedCallback( (notification, err) => {
        if (err) {
            console.error("failed to process received notificiation");
            showMessage("❌ Failed to receive notification");
        } else {
            console.log("received notificiation", notification);
            showMessage("📩 Push Received: " + JSON.stringify(notification));

        }
    });

    const pushOpenedCallbackId = await Pushwoosh.pushOpenedCallback( (notification, err) => {
        if (err) {
            console.error("failed to process opened notificiation");
            showMessage("❌ Failed to open notification");

        } else {
            console.log("opened notificiation", notification);
            showMessage("📬 Push Opened: " + JSON.stringify(notification));
        }
    });
    Pushwoosh.onDeviceReady({
        "projectid":"245850018966",
        "appid":"11C10-EF18D"
    });
    
    Pushwoosh.setMultiNotificationMode()
    .then(console.log("MultiNotificationMode set"));

    await Pushwoosh.registerDevice()
        .then(result => {
            console.log("Push token received:", result.pushToken);
        })
        .catch(error => {
            console.error("Failed to register device:", error);
        });

    console.log("🧪 Testing primitive parameter methods...");
    
    try {
        await Pushwoosh.setUserId("testUser123");
        console.log("✅ setUserId(string) - SUCCESS: User ID set to 'testUser123'");
    } catch (error) {
        console.error("❌ setUserId(string) - FAILED:", error);
    }
    
    try {
        await Pushwoosh.setLanguage("es");
        console.log("✅ setLanguage(string) - SUCCESS: Language set to 'es'");
    } catch (error) {
        console.error("❌ setLanguage(string) - FAILED:", error);
    }
    
    try {
        await Pushwoosh.setApiToken("test_api_token_123456");
        console.log("✅ setApiToken(string) - SUCCESS: API token set to 'test_api_token_123456'");
    } catch (error) {
        console.error("❌ setApiToken(string) - FAILED:", error);
    }
    
    const eventAttributes = {
        username: "john_doe",
        platform: "capacitor",
        test_type: "primitive_params",
        timestamp: new Date().toISOString()
    };
    
    try {
        await Pushwoosh.postEvent("testEvent", eventAttributes);
        console.log("✅ postEvent(string, object) - SUCCESS: Event 'testEvent' posted with attributes:", eventAttributes);
    } catch (error) {
        console.error("❌ postEvent(string, object) - FAILED:", error);
    }

    // // await Pushwoosh.unregisterDevice()
    // //     .then(result => {
    // //         console.log("Successfully unregistered"); // Handle unregistration (e.g., print to logs)
    // //     })
    // //     .catch(error => {
    // //         console.error("Failed to unregister device:", error); // Handle the error (e.g., print error message)
    // //     });

    // const config = {
    //     user_id: '12345',               // string
    //     age: 25,                        // number
    //     interests: ['sports', 'music'], // string[]
    //     scores: [95, 88, 76],           // number[]
    // };

    // await Pushwoosh.setTags(config)
    //     .then(result => {
    //         console.log("Tags set"); // Handle successful callback (e.g., print to logs)
    //     })
    //     .catch(error => {
    //         console.error("Failed to set tags:", error); // Handle the error (e.g., print error message)
    //     });

    // const tags = await Pushwoosh.getTags()
    // .then(result => {
    //     // result is the object itself, which is of type PushwooshTags
    //     for (const key in result) {
    //         if (result.hasOwnProperty(key)) {
    //             const value = result[key];

    //             if (typeof value === 'string') {
    //                 console.log(`${key}: ${value}`); // Log string values
    //             } else if (typeof value === 'number') {
    //                 console.log(`${key}: ${value}`); // Log number values
    //             } else if (Array.isArray(value)) {
    //                 // Log array values, either string[] or number[]
    //                 console.log(`${key}: [${value.join(', ')}]`); // Join array items as a string
    //             }
    //         }
    //     }
    // })
    // .catch(error => {
    //     console.error("Failed to get tags:", error); // Handle the error (e.g., print error message)
    // });

    const pushToken = await Pushwoosh.getPushToken();
    const hwid = await Pushwoosh.getPushwooshHWID();
    const remoteNotificationStatus = await Pushwoosh.getRemoteNotificationStatus();
    console.log ("Hwid: " + hwid.value + ", Token: " + pushToken.value + ", Status: " + remoteNotificationStatus.status);

    try {
        await Pushwoosh.setApplicationIconBadgeNumber(5);
        console.log("✅ setApplicationIconBadgeNumber(number) - SUCCESS: Badge set to 5");
    } catch (error) {
        console.error("❌ setApplicationIconBadgeNumber(number) - FAILED:", error);
    }
    
    try {
        await Pushwoosh.addToApplicationIconBadgeNumber(-2);
        console.log("✅ addToApplicationIconBadgeNumber(number) - SUCCESS: Badge decreased by 2");
    } catch (error) {
        console.error("❌ addToApplicationIconBadgeNumber(number) - FAILED:", error);
    }
    
    try {
        const badges = await Pushwoosh.getApplicationIconBadgeNumber();
        console.log("📱 Final badge value:", badges.badge || badges.value);
    } catch (error) {
        console.error("❌ getApplicationIconBadgeNumber - FAILED:", error);
    }

    const launchNotification = await Pushwoosh.getLaunchNotification();
    Pushwoosh.clearLaunchNotification();

    try {
        await Pushwoosh.setCommunicationEnabled(true);
        console.log("✅ setCommunicationEnabled(boolean) - SUCCESS: Communication enabled");
    } catch (error) {
        console.error("❌ setCommunicationEnabled(boolean) - FAILED:", error);
    }
    
    try {
        await Pushwoosh.setCommunicationEnabled(false);
        console.log("✅ setCommunicationEnabled(boolean) - SUCCESS: Communication disabled");
    } catch (error) {
        console.error("❌ setCommunicationEnabled(boolean) - FAILED:", error);
    }
    
    try {
        await Pushwoosh.setCommunicationEnabled(true);
        console.log("✅ setCommunicationEnabled(boolean) - SUCCESS: Communication re-enabled");
    } catch (error) {
        console.error("❌ setCommunicationEnabled(boolean) - FAILED:", error);
    }
}
