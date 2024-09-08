package Parser.RedisTypes;

import java.util.*;

public class RESPStream {
    static private List<RespStreamEntry> entries = new ArrayList<>();

    public RESPStream() {}

    public RespStreamEntry getStreamEntry(String streamEntryId) {
        return entries.stream()
          .filter(se -> se.getId().equals(streamEntryId))
          .findFirst()
          .orElse(null);
    }

    public RespStreamEntry getLastStreamEntry() {
        return entries.getLast();
    }

    public void addStreamEntry(RespStreamEntry streamEntry) {
        entries.add(streamEntry);
    }

    public String isStreamEntryValid(String streamEntryId) {
        if (entries.isEmpty()) {
            return "";
        }

        RespStreamEntry lastStreamEntry = getLastStreamEntry();
        long newStreamIdMillisecondsPart = Long.parseLong(streamEntryId.split("-")[0]);
        long lastStreamIdMillisecondsPart = Long.parseLong(lastStreamEntry.streamIdSplit().getFirst());
        long newStreamIdSequenceNoPart = Long.parseLong(streamEntryId.split("-")[1]);
        long lastStreamIdSequenceNoPart = Long.parseLong(lastStreamEntry.streamIdSplit().getLast());

        if (newStreamIdMillisecondsPart == 0 && newStreamIdSequenceNoPart == 0) {
            return "-ERR The ID specified in XADD must be greater than 0-0\r\n";
        }

        boolean isMilliSecondPartValid = newStreamIdMillisecondsPart >= lastStreamIdMillisecondsPart;
        boolean isSequenceNumberValid = newStreamIdSequenceNoPart >= lastStreamIdSequenceNoPart;;
        if (newStreamIdMillisecondsPart == lastStreamIdMillisecondsPart) {
            isSequenceNumberValid = newStreamIdSequenceNoPart > lastStreamIdSequenceNoPart;
        }

        if (!isMilliSecondPartValid || !isSequenceNumberValid) {
            return "-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n";
        }
        return "";
    }

    public class RespStreamEntry {
        String id;
        List<Map<String, Object>> streamEntries = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<Map<String, Object>> getStreamEntries() {
            return streamEntries;
        }

        public void addStreamEntry(Map<String, Object> streamEntry) {
            this.streamEntries.add(streamEntry);
        }

        public List<String> streamIdSplit() {
            return Arrays.stream(id.split("-")).toList();
        }
    }


}
