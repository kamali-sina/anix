from random import random, randint
import copy

from rz_settings import *
import rz_data_holder
from rz_message import Message

class User:
    def __init__(self, id: int, location: tuple, world_dimension: tuple, is_adversary=False) -> None:
        self.id = id
        self.is_adversary = is_adversary
        self.contacts = {}
        self.message_storage = []
        self.world_dimension = world_dimension
        self.location = location
        self.seen_misinformation = {}

        # TODO: to delete
        self.people_to_owt = []
        self.inbox_owt_count = 0
        self.pending_incoming_owts = {}
        self.outgoing_owt_dict = {}

        self.unknown_messages_dict = {}
    
    def extend_contacts(self, contact_list: list) -> None:
        if self.is_adversary:
            self._adversary_extend_contacts(contact_list)
        else:
            for contact in contact_list:
                self.contacts[contact] = True

    def _adversary_extend_contacts(self, contact_list: list) -> None:
        new_friend_count = min(len(contact_list) * ADVERSARY_FRIEND_REDUCTION, len(contact_list))
        for i in range(len(contact_list)):
            if i >= new_friend_count: break
            contact = contact_list[i]
            self.contacts[contact] = True

    def generate_message(self, step):
        if self.is_adversary:
            self.generate_adversary_message(step)
        elif random() < USER_MESSAGE_CREATION_RATE:
            self.generate_normal_message(step)

    def add_messages(self, messages, trust_ratio, step):
        message_id_set = set()
        for message in self.message_storage:
            message_id_set.add(message.id)
        
        for message in messages:
            rz_data_holder.messages_exchanged_steps[step] += 1
            if message.id not in message_id_set:
                message_copy = copy.deepcopy(message)
                message_copy.decrease_ttl()
                message_copy.received_at = step
                message_copy.trust_score = message_copy.trust_score * trust_ratio
                # message_copy.seen_by[self.id] = True
                if self.id not in rz_data_holder.message_seen_counter[message.id]:
                    rz_data_holder.message_seen_counter[message_copy.id].add(self.id)
                    rz_data_holder.message_seen_list_holder[message_copy.id][step] += 1
                self._check_for_percentiles(message_copy, step)
                self.message_storage.append(message_copy)

    def _check_for_percentiles(self, message, step):
        if len(rz_data_holder.message_seen_counter[message.id]) / N > rz_data_holder.highest_percentile_reached_for_message:
            rz_data_holder.highest_percentile_reached_for_message = len(rz_data_holder.message_seen_counter[message.id]) / N

        if (len(rz_data_holder.message_seen_counter[message.id]) / N) > 0.8 and message.id not in rz_data_holder.message_80percentile_holder:
            rz_data_holder.message_propagation_times_80_percentile.append(step - message.created_at)
            rz_data_holder.message_80percentile_holder.add(message.id)
        elif (len(rz_data_holder.message_seen_counter[message.id]) / N) > 0.9 and message.id not in rz_data_holder.message_90percentile_holder:
            rz_data_holder.message_propagation_times_90_percentile.append(step - message.created_at)
            rz_data_holder.message_90percentile_holder.add(message.id)
        elif (len(rz_data_holder.message_seen_counter[message.id]) / N) >= 0.999 and message.id not in rz_data_holder.message_fullpercentile_holder:
            rz_data_holder.message_propagation_times_full.append(step - message.created_at)
            rz_data_holder.message_fullpercentile_holder.add(message.id)

    def generate_adversary_message(self, step):
        self.message_storage.append(Message(self.id, step, is_misinformation=True))
        rz_data_holder.misinformation_count[step] += 1

    def generate_normal_message(self, step):
        self.message_storage.append(Message(self.id, step, is_misinformation=False))
    
    def move(self, move_range):
        # Random move value within move_range
        move_value = (randint(move_range[0][0], move_range[0][1]), randint(move_range[1][0], move_range[1][1]))
        # This line bounds the new location to the boundries of the map
        self.location = (max(min(self.location[0] + move_value[0], self.world_dimension[0] - 1), 0), max(min(self.location[1] + move_value[1], self.world_dimension[1] - 1), 0))

    def act(self, step):
        if self.is_adversary:
            self._adversary_act()
        else:
            self._benign_act(step)

    # TODO: UPDATE ALL THE ACTS ACCORDING TO RANGZEN
    """
    Take a look at the rangzen paper. Trust scores should somehow become priorities
    There is a sigmoid function that was applied to the trust scores that we currently did not use.
    """

    def _adversary_act(self):
        # for message in self.message_storage:
        #     if message.is_owt and message.owt_recipient == self.id:
        #         rz_data_holder.owts_recieved_by_adversaries.add(message.author)
        pass

    def _benign_act(self, step):
        self._delete_extra_messages(step)
    
    def _delete_extra_messages(self, step):
        # if len(self.message_storage) > MESSAGE_STORAGE_SIZE:
        #     self.message_storage[:] = [message for message in self.message_storage if self._determine_if_is_recent_enough(message, step)]
        if len(self.message_storage) > MESSAGE_STORAGE_SIZE:
            self.message_storage.sort(key=lambda x: x.trust_score, reverse=True)
            to_be_deleted = len(self.message_storage) - MESSAGE_STORAGE_SIZE
            self.message_storage = self.message_storage[:-to_be_deleted]
        # self.unknown_messages_dict.clear()
        self.update_seen_misinformation()
    
    def _determine_if_is_recent_enough(self, message, step):
        should_be_kept = True
        if step - message.received_at > OLD_MESSAGE_CUTOFF:
            should_be_kept = False
        return should_be_kept

    def update_seen_misinformation(self):
        for message in self.message_storage:
            if self.id in rz_data_holder.message_trust_scores_for_all_messages[message.id]:
                rz_data_holder.message_trust_scores_for_all_messages[message.id][self.id] = max(message.trust_score, rz_data_holder.message_trust_scores_for_all_messages[message.id][self.id])
            else:
                rz_data_holder.message_trust_scores_for_all_messages[message.id][self.id] = message.trust_score

    def _determine_if_is_useful(self, message, step):
        if self._internal_deletion_counter < 0: return True
        should_be_kept = True
        if message.id in self.unknown_messages_dict:
            should_be_kept = False
            self._internal_deletion_counter -= 1
        
        return should_be_kept
