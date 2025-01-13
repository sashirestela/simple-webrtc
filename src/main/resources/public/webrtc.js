// OpenAI API url for realtime service
const openAIUrl = "https://api.openai.com/v1/realtime";
// OpenAI Realtime model
const model = "gpt-4o-mini-realtime-preview";
// Flag indicating whether WebRTC is active, controls the enabling and disabling of connections
let isWebRTCActive = false;

// Create variables related to the WebRTC connection
let peerConnection;
let dataChannel;

// Get the control button element
const toggleButton = document.getElementById('toggleWebRTCButton');
// Add a click event listener to the button to toggle the WebRTC connection state
toggleButton.addEventListener('click', () => {
  // If WebRTC is active, stop the connection; otherwise, start WebRTC
  if (isWebRTCActive) {
    stopWebRTC(); // Stop WebRTC
    toggleButton.textContent = 'Start'; // Update button text
  } else {
    startWebRTC(); // Start WebRTC
    toggleButton.textContent = 'Stop'; // Update button text
  }
});

// Capture microphone input stream and initiate WebRTC connection
function startWebRTC() {
  // If WebRTC is already active, return directly
  if (isWebRTCActive) return;
  // Create a new peerConnection object to establish a WebRTC connection
  peerConnection = new RTCPeerConnection();
  peerConnection.ontrack = handleTrack; // Bind audio stream processing function
  createDataChannel(); // Create data channel
  // Request user's audio stream
  navigator.mediaDevices.getUserMedia({ audio: true }).then((stream) => {
    // Add each track from the audio stream to the peerConnection
    stream.getTracks().forEach((track) => peerConnection.addTransceiver(track, { direction: 'sendrecv' }));
    // Create an offer for the local connection
    peerConnection.createOffer().then(async (offer) => {
      peerConnection.setLocalDescription(offer); // Set local description (offer)
      // Get ephemeral key
      const ephemeralKey = await getEphemeralKey();
      // Send the offer to the backend for signaling exchange
      fetch(`${openAIUrl}?model=${model}`, {
        method: 'POST',
        body: offer.sdp, // Send the SDP of the offer to the backend
        headers: {
          Authorization: `Bearer ${ephemeralKey}`,
          'Content-Type': 'application/sdp',
        },
      })
        .then((r) => r.text())
        .then((answer) => {
          // Get the answer returned and set it as the remote description
          peerConnection.setRemoteDescription({ sdp: answer, type: 'answer' });
        });
    });
  });
  // Mark WebRTC as active
  isWebRTCActive = true;
}

// Stop the WebRTC connection and clean up all resources
function stopWebRTC() {
  // If WebRTC is not active, return directly
  if (!isWebRTCActive) return;
  // Stop the received audio tracks
  const tracks = peerConnection.getReceivers().map(receiver => receiver.track);
  tracks.forEach(track => track.stop());
  // Close the data channel and WebRTC connection
  if (dataChannel) dataChannel.close();
  if (peerConnection) peerConnection.close();
  // Reset connection and channel objects
  peerConnection = null;
  dataChannel = null;
  // Mark WebRTC as not active
  isWebRTCActive = false;
}

// When an audio stream is received, add it to the page and play it
function handleTrack(event) {
  const el = document.createElement('audio'); // Create an audio element
  el.srcObject = event.streams[0]; // Set the audio stream as the element's source
  el.autoplay = el.controls = true; // Autoplay and display audio controls
  document.body.appendChild(el); // Add the audio element to the page
}

// Create a data channel for transmitting control messages
function createDataChannel() {
  // Create a data channel named 'response'
  dataChannel = peerConnection.createDataChannel('oai-events');
  // Configure data channel events
  dataChannel.addEventListener('open', () => {
    console.log('Data channel opened');
    configureData(); // Configure data channel
  });
  dataChannel.addEventListener('message', async (ev) => {
    console.log(ev); // Logging received events
  });
}

// Configure data channel
function configureData() {
  console.log('Configuring data channel');
  const event = {
    type: 'session.update', // Session update event
    session: {
      modalities: ['text', 'audio'], // Supported interaction modes: text and audio
      instructions: 'Respond with short, direct sentences.',
      voice: 'ash',
    },
  };
  dataChannel.send(JSON.stringify(event)); // Send the configured event data
}

// Get an ephemeral key from your server
async function getEphemeralKey() {
  const tokenResponse = await fetch('/sessions', {
    method: 'POST',
    body: JSON.stringify({
      model: model,
      modalities: ['text', 'audio'],
      instructions: 'Respond with short, direct sentences.',
      voice: 'ash',
    }),
  });
  const data = await tokenResponse.json();
  console.log(data);
  return data.value;
}