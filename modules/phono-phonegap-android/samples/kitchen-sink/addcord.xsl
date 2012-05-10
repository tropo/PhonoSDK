<?xml version="1.0" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

   <!-- IdentityTransform -->
   <xsl:template match="/ | @* | node()">
         <xsl:copy>
               <xsl:apply-templates select="@* | node()" />
         </xsl:copy>
   </xsl:template>
   <xsl:template match="script[@src='jquery_1_4_2.js']">
           <script type="text/javascript" src="cordova-1.7.0.js"></script><xsl:text>
</xsl:text>
         <xsl:copy>
               <xsl:apply-templates select="@* | node()" />
         </xsl:copy>
   </xsl:template>
   <xsl:template match="script[@src='kitchen-sink.js']">
	<script type="text/javascript" charset="utf-8">
<xsl:text>
    // Call onDeviceReady when PhoneGap is loaded.
    //
    // At this point, the document has loaded but phonegap-1.0.0.js has not.
    // When PhoneGap is loaded and talking with the native device,
    // it will call the event `deviceready`.
    // 
    document.addEventListener("deviceready", onDeviceReady, false);

    // PhoneGap is loaded and it is now safe to make calls PhoneGap methods
    //
    function onDeviceReady() {
	$.getScript("kitchen-sink.js", function(data, textStatus, jqxhr) {
   		console.log('Loaded the kitchen-sink.js');
        });
    }
</xsl:text>
           </script>
   </xsl:template>


</xsl:stylesheet>
