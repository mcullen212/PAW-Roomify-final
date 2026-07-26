import {
    AirVent,
    BedDouble,
    BedSingle,
    Building2,
    Car,
    Dumbbell,
    Heater,
    Home,
    User,
    Users,
    Waves,
    Wifi,
} from "lucide-react";

export type { Review } from "~/lib/interfaces/reviews";

export const AMENITY_MAP: Record<string, React.ReactNode> = {
    "WIFI": <Wifi className="w-5 h-5" />,
    "AC": <AirVent className="w-5 h-5" />,
    "HEATING": <Heater className="w-5 h-5" />,
    "PARKING": <Car className="w-5 h-5" />,
    "POOL": <Waves className="w-5 h-5" />,
    "GYM": <Dumbbell className="w-5 h-5" />
};

export const BED_TYPE_MAP: Record<string, React.ReactNode> = {
    "TWIN": <BedSingle className="w-4 h-4 mr-1" />,
    "QUEEN": <BedDouble className="w-4 h-4 mr-1" />,
    "KING": <BedDouble className="w-4 h-4 mr-1" />
};

export const ROOM_TYPE_MAP: Record<string, React.ReactNode> = {
    "HOME": <Home className="w-4 h-4 mr-1" />,
    "PRIVATE": <User className="w-4 h-4 mr-1" />,
    "SHARED": <Users className="w-4 h-4 mr-1" />,
    "STUDIO": <Building2 className="w-4 h-4 mr-1" />
};

export const DEFAULT_AMENITY_ICON = <Home className="w-5 h-5" />;
export const DEFAULT_BED_TYPE_ICON = <BedDouble className="w-4 h-4 mr-1" />;
export const DEFAULT_ROOM_TYPE_ICON = <Building2 className="w-4 h-4 mr-1" />;
