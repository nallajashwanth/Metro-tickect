package com.example.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MetroLine(
    val colorHex: Long,
    val displayNameEn: String,
    val displayNameTe: String,
    val displayNameHi: String
) {
    RED(0xFFE53935, "Red Line", "ఎరుపు లైన్", "लाल लाइन"),
    BLUE(0xFF1E88E5, "Blue Line", "నీలం లైన్", "नीली लाइन"),
    GREEN(0xFF4CAF50, "Green Line", "ఆకుపచ్చ లైన్", "हरी लाइन")
}

data class MetroStation(
    val name: String,
    val nameTe: String,
    val nameHi: String,
    val line: MetroLine,
    val order: Int,
    val lat: Double, // Approx lat for distance calculations
    val lon: Double, // Approx lon for distance calculations
    val isInterchange: Boolean = false
)

object MetroNetwork {
    // List of all stations in Hyderabad Metro with approx coordinates
    val stations = listOf(
        // --- RED LINE ---
        MetroStation("Miyapur", "మియాపూర్", "मियापुर", MetroLine.RED, 1, 17.4968, 78.3614),
        MetroStation("JNTU College", "జేఎన్‌టీయూ కాలేజ్", "जेएनटीयू कॉलेज", MetroLine.RED, 2, 17.4912, 78.3815),
        MetroStation("KPHB Colony", "కేపీహెచ్‌బీ కాలనీ", "केपीएचबी कॉलोनी", MetroLine.RED, 3, 17.4846, 78.3888),
        MetroStation("Kukatpally", "కూకట్‌పల్లి", "कूकटपल्ली", MetroLine.RED, 4, 17.4727, 78.3995),
        MetroStation("Moosapet", "మూసాపేట్", "मूसापेट", MetroLine.RED, 5, 17.4646, 78.4121),
        MetroStation("Dr. B. R. Ambedkar Balanagar", "బాలానగర్", "बालानगर", MetroLine.RED, 6, 17.4608, 78.4231),
        MetroStation("Bharat Nagar", "భారత్ నగర్", "भारत नगर", MetroLine.RED, 7, 17.4589, 78.4326),
        MetroStation("Erragadda", "ఎర్రగడ్డ", "एरागाडा", MetroLine.RED, 8, 17.4542, 78.4384),
        MetroStation("ESI Hospital", "ఈఎస్‌ఐ హాస్పిటల్", "ईएसआई अस्पताल", MetroLine.RED, 9, 17.4479, 78.4414),
        MetroStation("S.R. Nagar", "ఎస్.ఆర్. నగర్", "एस.आर. नगर", MetroLine.RED, 10, 17.4429, 78.4449),
        MetroStation("Ameerpet", "అమీర్‌పేట్", "अमीरपेट", MetroLine.RED, 11, 17.4375, 78.4482, isInterchange = true),
        MetroStation("Punjagutta", "పంజాగుట్ట", "पंजागुट्टा", MetroLine.RED, 12, 17.4264, 78.4532),
        MetroStation("Irrum Manzil", "ఇరుమ్ మంజిల్", "इरुम मंज़िल", MetroLine.RED, 13, 17.4194, 78.4578),
        MetroStation("Khairatabad", "ఖైరతాబాద్", "खैराताबाद", MetroLine.RED, 14, 17.4116, 78.4611),
        MetroStation("Lakdi-ka-pul", "లక్డీకాపూల్", "लकड़ी का पुल", MetroLine.RED, 15, 17.4046, 78.4646),
        MetroStation("Assembly", "అసెంబ్లీ", "असेम्बली", MetroLine.RED, 16, 17.3996, 78.4696),
        MetroStation("Nampally", "నాంపల్లి", "नामपल्ली", MetroLine.RED, 17, 17.3916, 78.4736),
        MetroStation("Gandhi Bhavan", "గాంధీ భవన్", "गांधी भवन", MetroLine.RED, 18, 17.3856, 78.4746),
        MetroStation("Osmania Medical College", "ఉస్మానియా మెడికల్ కాలేజ్", "उस्मानिया मेडिकल कॉलेज", MetroLine.RED, 19, 17.3812, 78.4776),
        MetroStation("MG Bus Station", "మహాత్మా గాంధీ బస్ స్టేషన్", "महात्मा गांधी बस स्टेशन", MetroLine.RED, 20, 17.3789, 78.4812, isInterchange = true),
        MetroStation("Malakpet", "మలక్‌పేట్", "मलकपेट", MetroLine.RED, 21, 17.3776, 78.4902),
        MetroStation("New Market", "న్యూ మార్కెట్", "न्यू मार्केट", MetroLine.RED, 22, 17.3762, 78.4996),
        MetroStation("Musarambagh", "మూసారాంబాగ్", "मूसारामबाग", MetroLine.RED, 23, 17.3712, 78.5106),
        MetroStation("Dilsukhnagar", "దిల్‌సుఖ్‌నగర్", "दिलसुखनगर", MetroLine.RED, 24, 17.3686, 78.5244),
        MetroStation("Chaitanyapuri", "చైతన్యపురి", "चैतन्यपुरी", MetroLine.RED, 25, 17.3662, 78.5366),
        MetroStation("Victoria Memorial", "విక్టోరియా మెమోరియల్", "विक्टोरिया मेमोरियल", MetroLine.RED, 26, 17.3612, 78.5456),
        MetroStation("L.B. Nagar", "ఎల్.బి. నగర్", "एल.बी. नगर", MetroLine.RED, 27, 17.3536, 78.5524),

        // --- BLUE LINE ---
        MetroStation("Raidurg", "రాయదుర్గం", "रायदुर्ग", MetroLine.BLUE, 1, 17.4429, 78.3782),
        MetroStation("HITEC City", "హైటెక్ సిటీ", "हाईटेक सिटी", MetroLine.BLUE, 2, 17.4442, 78.3846),
        MetroStation("Durgam Cheruvu", "దుర్గం చెరువు", "दुर्गम चेरुवू", MetroLine.BLUE, 3, 17.4426, 78.3962),
        MetroStation("Madhapur", "మాదాపూర్", "माधापुर", MetroLine.BLUE, 4, 17.4398, 78.4042),
        MetroStation("Peddamma Gudi", "పెద్దమ్మ గుడి", "पेदम्मा गुड़ी", MetroLine.BLUE, 5, 17.4344, 78.4116),
        MetroStation("Jubilee Hills Check Post", "జుబిలీ హిల్స్ చెక్ పోస్ట్", "जुबली हिल्स चेक पोस्ट", MetroLine.BLUE, 6, 17.4316, 78.4192),
        MetroStation("Road No 5 Jubilee Hills", "రోడ్ నెం 5 జుబిలీ హిల్స్", "रोड नंबर 5 जुबली हिल्स", MetroLine.BLUE, 7, 17.4302, 78.4286),
        MetroStation("Yusufguda", "యూసుఫ్‌గూడ", "यूसुफ़गुड़ा", MetroLine.BLUE, 8, 17.4336, 78.4356),
        MetroStation("Madhura Nagar", "మధురా నగర్", "मधुरा नगर", MetroLine.BLUE, 9, 17.4354, 78.4412),
        // Ameerpet is RED-11 & BLUE-10
        // Begumpet
        MetroStation("Begumpet", "బేగంపేట్", "बेगमपेट", MetroLine.BLUE, 11, 17.4396, 78.4589),
        MetroStation("Prakash Nagar", "ప్రకాష్ నగర్", "प्रकाश नगर", MetroLine.BLUE, 12, 17.4422, 78.4682),
        MetroStation("Rasoolpura", "రసూల్‌పురా", "रसूलपुरा", MetroLine.BLUE, 13, 17.4436, 78.4776),
        MetroStation("Paradise", "ప్యారడైజ్", "पैराडाइज", MetroLine.BLUE, 14, 17.4452, 78.4862),
        MetroStation("Parade Ground", "పరేడ్ గ్రౌండ్", "परेड ग्राउंड", MetroLine.BLUE, 15, 17.4442, 78.4976, isInterchange = true),
        MetroStation("Secunderabad East", "సికింద్రాబాద్ ఈస్ట్", "सिकंदराबाद ईस्ट", MetroLine.BLUE, 16, 17.4382, 78.5024),
        MetroStation("Mettuguda", "మెట్టుగూడ", "मेट्टुगुडा", MetroLine.BLUE, 17, 17.4326, 78.5186),
        MetroStation("Tarnaka", "తార్నాక", "तारनाका", MetroLine.BLUE, 18, 17.4284, 78.5284),
        MetroStation("Habsiguda", "హబ్సిగూడ", "हब्सीगुड़ा", MetroLine.BLUE, 19, 17.4196, 78.5442),
        MetroStation("NGRI", "ఎన్జీఆర్ఐ", "एनजीआरआई", MetroLine.BLUE, 20, 17.4112, 78.5524),
        MetroStation("Stadium", "స్టేడియం", "स्टेडियम", MetroLine.BLUE, 21, 17.4056, 78.5582),
        MetroStation("Uppal", "ఉప్పల్", "उप्पल", MetroLine.BLUE, 22, 17.3986, 78.5636),
        MetroStation("Nagole", "నాగోల్", "नागोल", MetroLine.BLUE, 23, 17.3912, 78.5684),

        // --- GREEN LINE ---
        MetroStation("JBS Parade Ground", "జెబిఎస్ పరేడ్ గ్రౌండ్", "जेबीएस परेड ग्राउंड", MetroLine.GREEN, 1, 17.4462, 78.4972, isInterchange = true),
        MetroStation("Secunderabad West", "సికింద్రాబాద్ వెస్ట్", "सिकंदराबाद वेस्ट", MetroLine.GREEN, 2, 17.4392, 78.4932),
        MetroStation("Gandhi Hospital", "గాంధీ హాస్పిటల్", "गांधी अस्पताल", MetroLine.GREEN, 3, 17.4298, 78.4984),
        MetroStation("Musheerabad", "ముషీరాబాద్", "मुशीराबाद", MetroLine.GREEN, 4, 17.4226, 78.4996),
        MetroStation("RTC X Roads", "ఆర్‌టిసి క్రాస్ రోడ్స్", "आरटीसी क्रॉस रोड्स", MetroLine.GREEN, 5, 17.4132, 78.4976),
        MetroStation("Chikkadpally", "చిక్కడపల్లి", "चिक्कड़पल्ली", MetroLine.GREEN, 6, 17.4062, 78.4942),
        MetroStation("Narayanaguda", "నారాయణగూడ", "नारायणगुड़ा", MetroLine.GREEN, 7, 17.3989, 78.4908),
        MetroStation("Sultan Bazaar", "సుల్తాన్ బజార్", "सुल्तान बाज़ार", MetroLine.GREEN, 8, 17.3886, 78.4854)
        // MG Bus Station is RED-20 & GREEN-9
    )

    fun getStationByName(name: String): MetroStation? {
        if (name == "Ameerpet") {
            return stations.first { it.name == "Ameerpet" }
        }
        if (name == "MG Bus Station") {
            return stations.first { it.name == "MG Bus Station" }
        }
        if (name == "Parade Ground" || name == "JBS Parade Ground") {
            return stations.first { it.name == "Parade Ground" || it.name == "JBS Parade Ground" }
        }
        return stations.find { it.name.equals(name, ignoreCase = true) }
    }

    // Distance in km using Haversine formula
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Simple routing algorithm to find the station list, interchanges, fare, and duration
    fun findRoute(sourceName: String, destName: String): MetroRouteResult? {
        val src = getStationByName(sourceName) ?: return null
        val dst = getStationByName(destName) ?: return null

        if (src.name == dst.name) {
            return MetroRouteResult(
                stations = listOf(src),
                interchanges = emptyList(),
                distanceKm = 0.0,
                durationMin = 0,
                fare = 10.0,
                instructionsEn = "You are already at your destination.",
                instructionsTe = "మీరు ఇప్పటికే మీ గమ్యస్థానంలో ఉన్నారు.",
                instructionsHi = "आप पहले से ही अपने गंतव्य पर हैं।"
            )
        }

        // BFS / Simple direct or interchange finder
        // Since we have Red, Blue, and Green lines:
        // Interchange list:
        // Red and Blue -> Ameerpet
        // Red and Green -> MG Bus Station
        // Blue and Green -> Parade Ground (or JBS Parade Ground)

        val routeStations = mutableListOf<MetroStation>()
        val interchanges = mutableListOf<MetroStation>()

        var currentLine = src.line
        val targetLine = dst.line

        if (currentLine == targetLine) {
            // Direct path
            val sameLineStations = stations.filter { it.line == currentLine }.sortedBy { it.order }
            val startIdx = sameLineStations.indexOfFirst { it.name == src.name }
            val endIdx = sameLineStations.indexOfFirst { it.name == dst.name }
            if (startIdx < endIdx) {
                routeStations.addAll(sameLineStations.subList(startIdx, endIdx + 1))
            } else {
                routeStations.addAll(sameLineStations.subList(endIdx, startIdx + 1).reversed())
            }
        } else {
            // Needs 1-step interchange
            val interchangeStationName = when {
                (currentLine == MetroLine.RED && targetLine == MetroLine.BLUE) || (currentLine == MetroLine.BLUE && targetLine == MetroLine.RED) -> "Ameerpet"
                (currentLine == MetroLine.RED && targetLine == MetroLine.GREEN) || (currentLine == MetroLine.GREEN && targetLine == MetroLine.RED) -> "MG Bus Station"
                (currentLine == MetroLine.BLUE && targetLine == MetroLine.GREEN) || (currentLine == MetroLine.GREEN && targetLine == MetroLine.BLUE) -> {
                    if (currentLine == MetroLine.BLUE) "Parade Ground" else "JBS Parade Ground"
                }
                else -> "Ameerpet" // Default fallback
            }

            // Find interchange station objects
            val icSrcSide = stations.find { it.name == interchangeStationName && it.line == currentLine }
                ?: stations.first { it.isInterchange }
            val icDstSide = stations.find { it.name == interchangeStationName && it.line == targetLine }
                ?: stations.first { it.isInterchange }

            // Path 1: Src to IC
            val line1Stations = stations.filter { it.line == currentLine }.sortedBy { it.order }
            val sIdx = line1Stations.indexOfFirst { it.name == src.name }
            val ic1Idx = line1Stations.indexOfFirst { it.name == icSrcSide.name }
            if (sIdx <= ic1Idx) {
                routeStations.addAll(line1Stations.subList(sIdx, ic1Idx + 1))
            } else {
                routeStations.addAll(line1Stations.subList(ic1Idx, sIdx + 1).reversed())
            }

            // Path 2: IC to Dst
            val line2Stations = stations.filter { it.line == targetLine }.sortedBy { it.order }
            val ic2Idx = line2Stations.indexOfFirst { it.name == icDstSide.name }
            val dIdx = line2Stations.indexOfFirst { it.name == dst.name }
            if (ic2Idx <= dIdx) {
                routeStations.addAll(line2Stations.subList(ic2Idx + 1, dIdx + 1))
            } else {
                routeStations.addAll(line2Stations.subList(dIdx, ic2Idx).reversed())
            }

            interchanges.add(icSrcSide)
        }

        // Calculate metrics
        val totalStations = routeStations.size
        val distanceKm = totalStations * 1.3
        val durationMin = totalStations * 2 + (interchanges.size * 3)
        // Fare: Base 10 + 3 per station, capped at 60
        val fare = (10.0 + (totalStations - 1) * 3.0).coerceAtMost(60.0)

        // Generate instructions
        val terminalDstName = if (currentLine == dst.line) dst.name else {
            if (interchanges.isNotEmpty()) interchanges.first().name else dst.name
        }
        val instructionsEn = buildString {
            append("Board the train towards $terminalDstName. ")
            if (interchanges.isNotEmpty()) {
                append("Change trains at ${interchanges.first().name} for the ${dst.line.displayNameEn}. ")
            }
            append("Exit on the left side at ${dst.name}.")
        }
        val instructionsTe = buildString {
            append("$terminalDstName వైపు వెళ్లే రైలు ఎక్కండి. ")
            if (interchanges.isNotEmpty()) {
                append("${interchanges.first().nameTe} వద్ద ${dst.line.displayNameTe} కోసం రైలు మారండి. ")
            }
            append("${dst.nameTe} వద్ద ఎడమ వైపున నిష్క్రమించండి.")
        }
        val instructionsHi = buildString {
            append("$terminalDstName की ओर जाने वाली ट्रेन में चढ़ें। ")
            if (interchanges.isNotEmpty()) {
                append("${interchanges.first().nameHi} पर ${dst.line.displayNameHi} के लिए ट्रेन बदलें। ")
            }
            append("${dst.nameHi} पर बाईं ओर से बाहर निकलें।")
        }

        return MetroRouteResult(
            stations = routeStations,
            interchanges = interchanges,
            distanceKm = distanceKm,
            durationMin = durationMin,
            fare = fare,
            instructionsEn = instructionsEn,
            instructionsTe = instructionsTe,
            instructionsHi = instructionsHi
        )
    }
}

data class MetroRouteResult(
    val stations: List<MetroStation>,
    val interchanges: List<MetroStation>,
    val distanceKm: Double,
    val durationMin: Int,
    val fare: Double,
    val instructionsEn: String,
    val instructionsTe: String,
    val instructionsHi: String
)
