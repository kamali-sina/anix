from rz_settings import T
global misinformation_count, upvoted_misinformation_count, messages_exchanged_steps, votes_exchanged_steps, message_propagation_times_80_percentile, message_propagation_times_90_percentile, message_propagation_times_full, total_owt_created, total_owts_responded_to, adversary_count, message_seen_counter
global owt_ttl_when_received, owt_delay_when_received, message_seen_list_holder, message_votes_for_all_messages, owts_recieved_by_adversaries, misinformation_messages_fast_set
global message_80percentile_holder, message_90percentile_holder, message_fullpercentile_holder, message_trust_scores_for_all_messages
misinformation_count = [0] * T
upvoted_misinformation_count = [0] * T
downvoted_misinformation_count = [0] * T

messages_exchanged_steps = [0] * T
votes_exchanged_steps = [0] * T 

message_propagation_times_80_percentile = []
message_propagation_times_90_percentile = []
message_propagation_times_full = []

# total messages sent out
total_owt_created = 0
total_owts_responded_to = 0

adversary_count = 0

highest_percentile_reached_for_message = 0

message_seen_counter = {}
# PROGRAM RELATED
message_80percentile_holder = set()
message_90percentile_holder = set()
message_fullpercentile_holder = set()

message_seen_list_holder = {} # NEW
# for each message, there is a list of how many new users saw that particular message in each step
message_votes_for_all_messages = [] # list of dictionaries of votes
# Rangzen specific
message_trust_scores_for_all_messages = []
# ===============
owt_ttl_when_received = [] # NEW
owt_delay_when_received = [] # NEW

owts_recieved_by_adversaries = set()
misinformation_messages_fast_set = set()