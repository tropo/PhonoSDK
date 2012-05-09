OUTBOUND CALL:
1) pre-dial-delay: waits a specified time to dial
2) dial-up: Dials a given SIP URI
3) pre-hangup-delay: waits a specified time to hang up a call (-1 [cancel call before it's been answered], default: -2 [doesn't need to hangup])
4) disconnect: true (if call is up, disconnect connection)

INCOMING CALL:
1) post-incoming-answer-delay: waits a specified time to answer (-1: reject incoming call immediately, default:0 [answer incoming call immediately])
2) pre-hangup-delay: waits a specified time to hang up a call (-1 [cancel call before it's been answered], default: -2 [doesn't need to hangup])
3) disconnect: true (if call is up, disconnect connection)


--------------------
USE CASES:
--------------------
- Connects to a specified gateway using a client XMPP connection

Outbound Call:
1) 
- Waits a specified time
- Dials a given SIP URI
- Waits a specified time
- Hangs up
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com pre-dial-delay=5 dial-uri=sip:3366@login.zipdx.com pre-hangup-delay=5

2)
- Waits a specified time
- Dials a given SIP URI
- Cancel the call before it's been answered
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com pre-dial-delay=5 dial-uri=sip:3366@login.zipdx.com pre-hangup-delay=-1

3)
- Dials a given SIP URI
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com dial-uri=sip:3366@login.zipdx.com
 
4)
- Dials a given SIP URI
- Waits a specified time (parameter: pre-hangup-delay need to provide and should >=0)
- Disconnect TCP connection
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com dial-uri=sip:3366@login.zipdx.com disconnect=true pre-hangup-delay=0

------------------------------------------------------------------------------------------

Incoming Call:
1)
- Reject an incoming call immediately
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com post-incoming-answer-delay=-1

2) 
- Waits a specified time
- Answer the call
- Waits a specified time
- hangup the call
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com post-incoming-answer-delay=7 pre-hangup-delay=10

3)
- Answer the call
- Waits a specified time
- hangup the call
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com pre-hangup-delay=10

4)
- Answer the call
- Waits a specified time (parameter: pre-hangup-delay need to provide and should >=0)
- Disconnect TCP connection
Command-line: ./adl /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug/PhonoCommandLineAir-app.xml /Users/Evelyn/Works/Phono_Flex/workspaces/PhonoCommandLineAir/bin-debug -- gateway=gw.phono.com disconnect=true pre-hangup-delay=0

