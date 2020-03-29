const functions = require('firebase-functions');

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

const admin = require('firebase-admin');
admin.initializeApp();

function getCurrLocations() {
    var array = [];
    const now = admin.firestore.Timestamp.fromDate(new Date())
    const cols = admin.firestore()
        .collection('current_locations')
        .get()
        .then(snapshot => {
            snapshot.forEach(doc => {
                array.push(doc);
            })
            return array;
        })
}

function didEncounter(doc1, doc2) {
    const locat1 = doc1.data()["geopoint"];
    const locat2 = doc2.data()["geopoint"];

    var R = 6371e3;
    var p1 = locat1.latitude.toRadians();
    var p2 = locat2.latitude.toRadians();
    var d1 = (locat1.latitude - locat2.latitude).toRadians();
    var d2 = (locat1.longitude - locat2.longitude).toRadians();

    var a = Math.sin(d1/2) * Math.sin(d1/2) +
            Math.cos(p1) * Math.cos(p2) +
            Math.sin(d2/2) * Math.sin(d2/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    var distance = R * c;

    return distance < 20;
}

exports.checkForContacts = functions.firestore
    .document('user_data/{androidId}/geo_data/{data}')
    .onCreate((snap, context) => {
        if (snap) {
            const docs = await getCurrLocations();
            for (var i = 0; i < docs.length; i++) {
                for (var j = i; j < docs.length; j++) {
                    if (didEncounter(docs[i], docs[j])) {
                        const contact1 = admin.firestore()
                            .doc(`contacts/${docs[i].id}/data/${docs[j].id}`);
                        contact1.set(docs[j]);
                        const contact2 = admin.firestore()
                            .doc(`contacts/${docs[j].id}/data/${docs[i].id}`);
                        contact2.set(docs[j]);
                    }
                }
            }
        }
        return snap;
    });

exports.findContacts = functions.firestore
    .document('confirmed_cases/{androidId}')
    .onCreate((snap, context) => {
        if (snap) {
            const cols = admin.firestore()
                .collection('contacts')
                .doc(context.params.androidId)
                .collection('data')
                .get()
                .then(snapshot => {
                    snapshot.forEach(doc => {
                        
                    })
                })
        }
        return snap;
    });
