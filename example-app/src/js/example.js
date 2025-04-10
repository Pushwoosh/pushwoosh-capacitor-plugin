import { Pushwoosh } from 'pushwoosh-capacitor-plugin';

window.testEcho = async () => {
    const inputValue = document.getElementById("echoInput").value;
    Pushwoosh.echo({ value: inputValue });
    Pushwoosh.onDeviceReady({
        "projectid":"245850018966",
        "appid":"11C10-EF18D"
    });

    const pushReceivedCallbackId = await Pushwoosh.pushReceivedCallback( (notification, err) => {
        if (err) {
            console.error("failed to process received notificiation");
        } else {
            console.log("received notificiation", notification);
        }
    });

    const pushOpenedCallbackId = await Pushwoosh.pushOpenedCallback( (notification, err) => {
        if (err) {
            console.error("failed to process opened notificiation");
        } else {
            console.log("opened notificiation", notification);
        }
    });
    
    Pushwoosh.setMultiNotificationMode()
    .then(console.log("MultiNotificationMode set"));

    // await Pushwoosh.registerDevice()
    //     .then(result => {
    //         console.log("Push token received:", result.pushToken); // Handle the token (e.g., print to logs)
    //     })
    //     .catch(error => {
    //         console.error("Failed to register device:", error); // Handle the error (e.g., print error message)
    //     });

    // Pushwoosh.setUserId({userId: "testUser"});
    // Pushwoosh.setLanguage({language: "es"});

    // const eventAttributes = {
    //   username: "john_doe",
    //   platform: "web",
    //   success: "true"
    // };

    // Pushwoosh.postEvent({ event: "testEvent", attributes: eventAttributes});

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

    await Pushwoosh.setApplicationIconBadgeNumber({badge: 5});
    await Pushwoosh.addToApplicationIconBadgeNumber({badge: -3});
    const badges = await Pushwoosh.getApplicationIconBadgeNumber()
    .then(result => {
        console.log("Badge value is : ", result.value)
    })

    const launchNotification = await Pushwoosh.getLaunchNotification();
    Pushwoosh.clearLaunchNotification();

    Pushwoosh.showGDPRConsentUI();
    Pushwoosh.showGDPRDeletionUI();
    await Pushwoosh.setCommunicationEnabled( {enabled: true} )
    .then(result => { console.log("setCommunicationEnabled success")} )
    .catch(error => { console.log("setCommunicationEnabled failed", error.result)});

}
