import { Link, useLocation } from "react-router"
import { Button } from "@/components/ui/button";
import { User } from "lucide-react";
import { useAuth } from "~/lib/auth/useAuth";
import { cn } from "~/lib/utils";
import { useTranslation } from "react-i18next";

export function Navbar() {
    const { t } = useTranslation();
    const location = useLocation();
    const { authenticated, loading, logout } = useAuth();

    const handleLogout = () => {
        logout();
    };

    const navItems = [
        { labelKey: "navbar.host", to: "/host", paths: ["/host"] },
        { labelKey: "navbar.swaps", to: "/swaps", paths: ["/swaps"] },
        { labelKey: "navbar.rooms", to: "/my-rooms", paths: ["/my-rooms", "/room"] },
        { labelKey: "navbar.trips", to: "/trips", paths: ["/trips"] },
    ];

    const isActivePath = (paths: string[]) =>
        paths.some((path) => location.pathname === path || location.pathname.startsWith(`${path}/`));

    const navLinkClassName = (active: boolean) =>
        cn(
            "relative py-2 transition-colors hover:text-primary",
            active &&
            "font-semibold text-primary after:absolute after:inset-x-0 after:-bottom-1 after:h-0.5 after:rounded-full after:bg-primary"
        );

    return (
        <header className="border-b border-border bg-background/95 backdrop-blur sticky top-0 z-50">
            <div className="container mx-auto px-4 py-4 flex items-center justify-between">
                <Link to="/" className="flex items-center gap-2 text-2xl font-bold">
                    <img src={`${import.meta.env.BASE_URL}favicon.png`} alt="Logo" className="w-8 h-8 object-contain" />
                    <span className="text-primary">Roomify</span>
                </Link>

                <nav className="flex items-center gap-6">
                    {!loading && (authenticated ? (
                        <>
                            <div className="hidden md:flex items-center gap-6 text-sm font-medium">
                                {navItems.map((item) => (
                                    <Link
                                        key={item.to}
                                        to={item.to}
                                        className={navLinkClassName(isActivePath(item.paths))}
                                    >
                                        {t(item.labelKey)}
                                    </Link>
                                ))}
                            </div>

                            <div className="flex items-center gap-3 ml-4 border-l pl-6 border-border">
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    asChild
                                    className={cn(
                                        "rounded-full hover:bg-secondary hover:text-primary hover:shadow-sm",
                                        isActivePath(["/profile"]) && "bg-secondary text-primary hover:bg-secondary/80 hover:text-primary"
                                    )}
                                >
                                    <Link to="/profile" aria-label="Profile">
                                        <User className="w-5 h-5" />
                                    </Link>
                                </Button>
                                <Button
                                    variant="ghost"
                                    className="text-sm font-semibold hover:bg-secondary hover:text-primary hover:shadow-sm"
                                    onClick={handleLogout}
                                >
                                    {t("navbar.logout")}
                                </Button>
                            </div>
                        </>
                    ) : (
                        <div className="flex items-center gap-3">
                            <Button variant="ghost" asChild><Link to="/login">{t("navbar.login")}</Link></Button>
                            <Button className="bg-blue-600 hover:bg-blue-700" asChild>
                                <Link to="/signUp">{t("navbar.signup")}</Link>
                            </Button>
                        </div>
                    ))}
                </nav>
            </div>
        </header>
    );
}
