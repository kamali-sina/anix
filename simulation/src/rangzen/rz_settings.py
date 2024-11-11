T = 120 # Steps Internet blackout = 5days 5 * 24 / 120 = 1 hours
N = 600 # users
A = 25 # X
B = 25 # Y
WORLD_DIMENSION = (A, B)
r = 2
MOVE_RANGE = ((-r, r), (-r, r))
d = 1
MESSAGE_EXCHANGE_RANGE = ((-d, d), (-d, d))
ADVERSARY_RATIO = 0.02 # TODO: new

# TODO: new
JAMMING_ATTACK = False
RANGZEN_EPSILON = 0.01

# Watts-Strogatz model parameters
WS_DELTA = 15
# WS_DELTA = N // 10
WS_BETA = 0.5

# User settings
MESSAGE_STORAGE_SIZE = 5000 # TODO: new
MAX_VOTES_ALLOWED_ON_MESSAGE = 600 # 3 GB

# (10000 + 10000)
# 1000 rest 
# 19000 junk |

MIN_TTL = T + 20
USER_MESSAGE_CREATION_RATE = 0.05 # Fixed
USER_ACT_PROBABILITY = 0.15 # Fixed Write all the reasoning

OWT_MIN_TRUST_VALUE = 4 # [[[[[[[Code]]]]]]] Fixed Change to ratio, for the scale: i.e 1000UP 995 D should not be trusted
OWT_MIN_UP_RATIO = 0.66 #60 percent upvotes
# 10 UP - 6 D
UPVOTE_MIN_TRUST_VALUE = 2 # Fixed # TODO: new
UPVOTE_MIN_UP_RATIO = 0.55 # FIXED # TODO: new

# Go for 3 examples and try to describe those

# Talk about adversary trying to tweak the trust scores |

# leave red message to mention trust does not get calcualted if we don't don't know the  don't know the voters
# don't know the author
# don't know the voters

OLD_MESSAGE_CUTOFF = 24 # hours
# mention in both design 4.5 (how the message storage works) and  implementation 5.2
#Add information about the owt handling (out going and incoming) and how we treat the older ones (do we use the same cutoff or not)

# [[[PROGRAMMING]]] Fix: the created_at thing with a received at for the class and that's what the old message cutoff should be calculated on
# TODO: [[[PROGRAMMING]]] Fix the how many adversary messages were trusted field
# Program related
UPVOTE = True
DOWNVOTE = False

TEST_NAME = "STANDARD"
ADVERSARY_FRIEND_REDUCTION = 0.4 
USER_VOTING_ON_UNKNOWN_MESSAGES_RATE = 0.05 
USER_UPVOTING_ON_MISINFORMATION_RATE = 0.3 
USER_UPVOTING_ON_NORMAL_RATE = 0.6 

# TEST_NAME = "GULLIBLE"
# ADVERSARY_FRIEND_REDUCTION = 0.7
# USER_VOTING_ON_UNKNOWN_MESSAGES_RATE = 0.1
# USER_UPVOTING_ON_MISINFORMATION_RATE = 0.4 
# USER_UPVOTING_ON_NORMAL_RATE = 0.55

# TEST_NAME = "SMART"
# ADVERSARY_FRIEND_REDUCTION = 0.2
# USER_VOTING_ON_UNKNOWN_MESSAGES_RATE = 0.02
# USER_UPVOTING_ON_MISINFORMATION_RATE = 0.2
# USER_UPVOTING_ON_NORMAL_RATE = 0.7


# TEST_NAME = "SUPER GULLIBLE"
# ADVERSARY_FRIEND_REDUCTION = 0.9
# USER_VOTING_ON_UNKNOWN_MESSAGES_RATE = 0.2
# USER_UPVOTING_ON_MISINFORMATION_RATE = 0.5 
# USER_UPVOTING_ON_NORMAL_RATE = 0.5

# TEST_NAME = "SUPER SMART"
# ADVERSARY_FRIEND_REDUCTION = 0.1
# USER_VOTING_ON_UNKNOWN_MESSAGES_RATE = 0.01
# USER_UPVOTING_ON_MISINFORMATION_RATE = 0.1
# USER_UPVOTING_ON_NORMAL_RATE = 0.8