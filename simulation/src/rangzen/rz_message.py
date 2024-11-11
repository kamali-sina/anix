from rz_settings import *
import rz_data_holder

class Message:
    ID_COUNTER = 0

    def __init__(self, author:int, step:int, is_misinformation:bool, is_owt=False, owt_recipient=-1) -> None:
        self.id = Message.ID_COUNTER
        rz_data_holder.message_votes_for_all_messages.append({})
        rz_data_holder.message_trust_scores_for_all_messages.append({})
        if is_misinformation:
            rz_data_holder.misinformation_messages_fast_set.add(self.id)
        Message.ID_COUNTER += 1
        self.author = author
        self.seen_by = {self.author: True}
        rz_data_holder.message_seen_counter[self.id] = set()
        rz_data_holder.message_seen_counter[self.id].add(self.author)
        rz_data_holder.message_seen_list_holder[self.id] = [0] * T
        rz_data_holder.message_seen_list_holder[self.id][step] += 1
        self.percentile_80 = False
        self.percentile_90 = False
        self.percentile_full = False
        self.created_at = step
        self.received_at = -1
        self.ttl = MIN_TTL
        self.is_misinformation = is_misinformation
        # RANGZEN UPDATE
        self.trust_score = 1.0
    
    def decrease_ttl(self):
        # self.ttl -= 1
        pass

